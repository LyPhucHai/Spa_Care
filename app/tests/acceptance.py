#!/usr/bin/env python3
"""Black-box API checks with a fresh H2 file; never touches the user's real DB."""
import concurrent.futures
import datetime as dt
import http.cookiejar
import json
import os
from pathlib import Path
import socket
import ssl
import subprocess
import tempfile
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
CHECKS = 0

def check(condition, message):
    global CHECKS
    if not condition:
        raise AssertionError(message)
    CHECKS += 1
    print('OK', message, flush=True)

class Client:
    def __init__(self, base, context=None):
        self.base = base
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies),urllib.request.HTTPSHandler(context=context))
        self.csrf = ''
    def request(self, path, body=None, expected=200, csrf=True, origin=None):
        headers = {'Content-Type': 'application/json'}
        if csrf:
            headers['X-CSRF-Token'] = self.csrf
        if origin:
            headers['Origin'] = origin
        req = urllib.request.Request(self.base + path, data=json.dumps(body).encode() if body is not None else None, headers=headers)
        try:
            response = self.opener.open(req, timeout=20)
        except urllib.error.HTTPError as e:
            response = e
        raw = response.read().decode('utf-8-sig')
        check(response.code == expected, f'{path} status {response.code} == {expected}: {raw[:150] if response.code != expected else ""}')
        if 'json' in response.headers.get('Content-Type', ''):
            return json.loads(raw)
        return raw
    def login(self, username, password='123456'):
        result = self.request('/api/login', {'username': username, 'password': password})
        self.csrf = result['csrf']
        return result
    def action(self, command, expected=200, **args):
        return self.request('/api/action', {'action': command, **args}, expected)
    def state(self):
        return self.request('/api/state')

class Server:
    def __init__(self, db, zone, tls=None):
        self.port = 18089
        self.log = tempfile.TemporaryFile(mode='w+')
        extra={} if tls is None else {'SPACARE_TLS_KEYSTORE':str(tls),'SPACARE_TLS_PASSWORD':'TestTls12345'}
        self.process = subprocess.Popen(['java', '-Duser.timezone='+zone, '-Dfile.encoding=UTF-8', '-cp', str(ROOT/'target/classes')+os.pathsep+str(ROOT/'lib/h2-2.2.224.jar'), 'spacare.web.SpaCareServer'], cwd=ROOT, env={**os.environ, 'PORT': str(self.port), 'SPACARE_DB': str(db), 'SPACARE_BACKUP_DIR': str(Path(db).parent/'backups'), **extra}, stdout=self.log, stderr=self.log)
        for _ in range(120):
            if self.process.poll() is not None:
                self.log.seek(0)
                raise RuntimeError(self.log.read())
            try:
                with socket.create_connection(('127.0.0.1', self.port), timeout=.2):
                    break
            except OSError:
                time.sleep(.1)
        else:
            self.stop()
            raise RuntimeError('Server did not become ready')
        self.base = ('http://' if tls is None else 'https://')+'127.0.0.1:'+str(self.port)
    def stop(self):
        self.process.terminate()
        try:
            self.process.wait(5)
        except subprocess.TimeoutExpired:
            self.process.kill()
        self.log.seek(0)
        logs = self.log.read()
        self.log.close()
        return logs

def run(base, day):
    anon = Client(base)
    anon.request('/api/state', expected=401)
    admin, desk, tech, customer = (Client(base) for _ in range(4))
    admin.login('admin'); desk.login('letan01'); tech.login('ktv_lan'); customer.login('khach01')
    check('password_hash' not in json.dumps(admin.state()), 'API state không trả password hash')
    check(len(customer.state()['customers']) == 1, 'Khách hàng chỉ thấy hồ sơ của mình')
    check(not tech.state()['customers'], 'Kỹ thuật viên không lấy được danh sách khách')
    admin.request('/api/action', {'action':'customer.save'}, expected=403, csrf=False)
    admin.request('/api/action', {'action':'customer.save'}, expected=403, origin='https://untrusted.example')
    customer.action('service.save', expected=403, name='Không được phép')
    desk.action('tier.save', expected=403, name='Không được phép')
    # Register does not allow role escalation.
    anon.request('/api/register', {'username':'newcustomer','password':'SafePass123','full_name':'Khách mới','phone':'0909999999','role':'ADMIN'}, expected=201)
    newcomer=Client(base); check(newcomer.login('newcustomer','SafePass123')['user']['role']=='CUSTOMER','Đăng ký không cho tự chọn quyền quản trị')
    anon.request('/api/register', {'username':'bad','password':'short','full_name':'Bad','phone':'0909999998'}, expected=400)
    cid=desk.action('customer.save', full_name='Khách tại quầy',phone='0905555555',email='',birthday='',allergy_note='')['id']
    desk.action('customer.save', expected=409, full_name='Trùng điện thoại',phone='0905555555')
    desk.action('customer.save', id=cid,full_name="Khách ' OR 1=1 --",phone='0905555555',birthday='',email='',allergy_note='Da nhạy cảm')
    # Main lifecycle, two distinct staff and rooms at the same time.
    start=day+'T09:00'
    appt=customer.action('appointment.create',service_ids=[1],branch_id=1,staff_id=1,customer_id=cid,start_at=start,group_count=1)['ids'][0]
    check(next(a for a in customer.state()['appointments'] if a['id']==appt)['customer_id']==1,'Khách đặt lịch luôn dùng hồ sơ chính mình')
    other=desk.action('appointment.create',service_ids=[1],branch_id=1,staff_id=2,customer_id=cid,start_at=start,group_count=1)['ids'][0]
    desk.action('appointment.create',expected=400,service_ids=[1],branch_id=1,staff_id=1,customer_id=cid,start_at=start)
    desk.action('appointment.create',expected=400,service_ids=[1],branch_id=1,staff_id=3,customer_id=cid,start_at=day+'T12:00')
    desk.action('appointment.create',expected=400,service_ids=[1],branch_id=2,staff_id=4,customer_id=1,start_at=start)
    desk.action('appointment.create',expected=400,service_ids=[],branch_id=1,staff_id=0,customer_id=cid,start_at=start)
    desk.action('appointment.create',expected=400,service_ids=[1],branch_id=2,staff_id=1,customer_id=cid,start_at=start)
    customer.action('appointment.cancel',expected=403,id=other,reason='Không có quyền')
    tech.action('appointment.status',expected=403,id=other,status='IN_PROGRESS')
    desk.action('appointment.status',expected=400,id=appt,status='COMPLETED')
    desk.action('invoice.pay',expected=400,appointment_id=appt,method='CASH')
    desk.action('appointment.status',id=appt,status='CHECKED_IN')
    tech.action('appointment.status',id=appt,status='IN_PROGRESS')
    # Insufficient supplies rolls back the status and all stock movements.
    admin.action('stock.adjust',product_id=1,quantity=-495,reason='Kiểm thử thiếu kho')
    tech.action('appointment.status',expected=400,id=appt,status='COMPLETED')
    check(next(a for a in desk.state()['appointments'] if a['id']==appt)['status']=='IN_PROGRESS','Thiếu vật tư không ghi trạng thái hoàn thành')
    admin.action('stock.adjust',product_id=1,quantity=500,reason='Bổ sung vật tư')
    tech.action('appointment.status',id=appt,status='COMPLETED')
    check(next(p for p in admin.state()['products'] if p['id']==1)['stock_qty']==495,'Kho trừ đúng định mức khi hoàn thành')
    tech.action('invoice.pay',expected=403,appointment_id=appt,method='CASH')
    quote=desk.action('invoice.preview',appointment_id=appt,method='CASH',promotion_code='SPACARE10',redeem_points=0,products=[{'product_id':2,'quantity':1}])
    check(quote['total']==720000 and quote['earned_points']==72,'Báo giá gồm sản phẩm và voucher chính xác')
    desk.action('invoice.pay',expected=400,appointment_id=appt,method='INVALID')
    desk.action('invoice.pay',expected=400,appointment_id=appt,method='CASH',redeem_points=10000)
    desk.action('invoice.pay',expected=400,appointment_id=appt,method='CASH',products=[{'product_id':2,'quantity':100}])
    check(not desk.state()['invoices'],'Thiếu kho khi bán lẻ rollback cả hóa đơn')
    paid=desk.action('invoice.pay',appointment_id=appt,method='CASH',promotion_code='SPACARE10',redeem_points=0,products=[{'product_id':2,'quantity':1}])
    check(paid['balance']==72,'Tích điểm và lưu sổ điểm cùng giao dịch')
    desk.action('invoice.pay',expected=400,appointment_id=appt,method='CASH')
    state=admin.state()
    check(state['commissions'][0]['amount']==45000,'Hoa hồng tính trên phần dịch vụ sau giảm, không cộng sản phẩm')
    check(sum(r['revenue'] for r in state['revenue'])==720000,'Báo cáo lấy số tiền thực thu')
    check(len(state['invoice_products'])==1,'Hóa đơn giữ dòng sản phẩm bán lẻ')
    customer.action('review.save',appointment_id=appt,rating=5,comment='Chăm sóc chu đáo')
    customer.action('review.save',expected=409,appointment_id=appt,rating=4,comment='Không lặp')
    customer.action('review.save',expected=403,appointment_id=other,rating=5,comment='Không được phép')
    # Reschedule preserves history, checks conflicts, and adjusts reminders.
    tomorrow=(dt.date.fromisoformat(day)+dt.timedelta(days=1)).isoformat()
    a=desk.action('appointment.create',service_ids=[3],branch_id=1,staff_id=2,customer_id=cid,start_at=tomorrow+'T09:00')['ids'][0]
    b=desk.action('appointment.create',service_ids=[3],branch_id=1,staff_id=2,customer_id=cid,start_at=tomorrow+'T10:00')['ids'][0]
    desk.action('appointment.reschedule',expected=400,id=a,start_at=tomorrow+'T10:00')
    desk.action('appointment.reschedule',id=a,start_at=tomorrow+'T11:00')
    desk.action('appointment.cancel',id=b,reason='Khách đổi kế hoạch')
    desk.action('appointment.status',expected=400,id=a,status='CHECKED_IN')
    check(next(x for x in desk.state()['appointments'] if x['id']==b)['status']=='CANCELLED','Hủy giải phóng lịch')
    # Treatment packages preserve remaining sessions and ownership.
    sold=desk.action('package.sell',customer_id=1,package_id=1,method='TRANSFER')
    cp=sold['id']
    desk.action('appointment.create',expected=400,service_ids=[1],branch_id=1,staff_id=1,customer_id=cid,start_at=tomorrow+'T13:00',customer_package_id=cp)
    pkgappt=desk.action('appointment.create',service_ids=[1],branch_id=1,staff_id=1,customer_id=1,start_at=day+'T13:00',customer_package_id=cp)['ids'][0]
    check(next(p for p in desk.state()['customer_packages'] if p['id']==cp)['available_sessions']==9,'Đặt lịch giữ một buổi liệu trình')
    for status in ['CHECKED_IN','IN_PROGRESS','COMPLETED']:
        desk.action('appointment.status',id=pkgappt,status=status)
    quote=desk.action('invoice.preview',appointment_id=pkgappt,method='CASH')
    check(quote['total']==0,'Dùng buổi liệu trình không thu tiền lại')
    desk.action('invoice.pay',appointment_id=pkgappt,method='CASH')
    check(next(p for p in desk.state()['customer_packages'] if p['id']==cp)['remaining_sessions']==9,'Thanh toán trừ đúng một buổi')
    # A group cannot overbook the one room in branch 2; whole group rolls back.
    count=len(desk.state()['appointments'])
    desk.action('appointment.create',expected=400,service_ids=[1],branch_id=2,staff_id=0,customer_id=cid,start_at=tomorrow+'T14:00',group_count=2)
    check(len(desk.state()['appointments'])==count,'Thiếu tài nguyên nhóm rollback toàn bộ nhóm')
    group=desk.action('appointment.create',service_ids=[1],branch_id=1,staff_id=0,customer_id=cid,start_at=tomorrow+'T14:00',group_count=2)['ids']
    check(len(group)==2,'Đi đôi tạo hai lịch có tài nguyên riêng')
    items=[i for i in desk.state()['items'] if i['appointment_id'] in group]
    check(len({i['staff_id'] for i in items})==2 and len({i['room_id'] for i in items})==2,'Nhóm không dùng trùng nhân viên/phòng')
    # Management CRUD and validation.
    s=admin.action('service.save',name='Dịch vụ mới',category='Facial',price=300000,duration_min=30,room_type='SPA',is_active=True)['id']
    staff=admin.action('staff.save',full_name='KTV mới',phone='0908888888',branch_id=1,commission_percent=15,is_active=True,service_ids=[s],username='newtech',password='NewTech123')['id']
    newtech=Client(base);newtech.login('newtech','NewTech123')
    admin.action('shift.save',staff_id=staff,work_date=tomorrow,start_time='08:00',end_time='12:00')
    admin.action('shift.save',expected=400,staff_id=staff,work_date=tomorrow,start_time='10:00',end_time='14:00')
    job=desk.action('appointment.create',service_ids=[s],branch_id=1,staff_id=staff,customer_id=cid,start_at=tomorrow+'T08:00')['ids'][0]
    shift=next(w['id'] for w in admin.state()['shifts'] if w['staff_id']==staff)
    admin.action('shift.delete',expected=400,id=shift)
    admin.action('service.save',id=s,name='Đổi giá',category='Facial',price=600000,duration_min=45,room_type='SPA',is_active=False)
    item=next(i for i in desk.state()['items'] if i['appointment_id']==job)
    check(item['price_at_booking']==300000,'Thay đổi giá giữ nguyên lịch sử')
    desk.action('appointment.create',expected=400,service_ids=[s],branch_id=1,staff_id=staff,customer_id=cid,start_at=tomorrow+'T10:00')
    newtech.action('attendance.toggle');newtech.action('attendance.toggle');newtech.action('attendance.toggle',expected=400)
    admin.action('combo.save',name='Combo kiểm thử',discount_percent=25,is_active=True,service_ids=[1,3])
    admin.action('promotion.save',code='TEST20',discount_percent=20,start_date=day,end_date=tomorrow,max_uses=5,min_total=0,is_active=True)
    admin.action('tier.save',name='PREMIUM',min_spending=100000000,discount_percent=15,multiplier=2)
    product=admin.action('product.save',name='Vật tư mới',sku='TEST-SKU',unit='ml',sale_price=1000,reorder_level=10,is_active=True)['id']
    admin.action('stock.adjust',product_id=product,quantity=50,reason='Nhập ban đầu')
    admin.action('stock.adjust',expected=400,product_id=product,quantity=-100,reason='Không đủ')
    admin.action('recipe.save',service_id=3,product_id=product,quantity=2)
    admin.action('package.save',name='Gói mới',service_id=3,total_sessions=3,price=1000000,valid_days=90,is_active=True)
    csv=admin.request('/api/export?from='+day+'&to='+day)
    check('4220000' in csv,'Xuất CSV bao gồm doanh thu thật và bán gói, không đếm lại buổi dùng')
    customer.request('/api/export',expected=403)
    admin.request('/api/state?from='+tomorrow+'&to='+day,expected=400)
    before=sum(r['revenue'] for r in admin.state()['revenue'])
    check(before==4220000,'Doanh thu tổng không cộng lại buổi gói trả trước')
    # Upgrade comes from actual paid invoices, not an editable tier field.
    for _ in range(3): desk.action('package.sell', customer_id=cid, package_id=1, method='CASH')
    check(next(c for c in desk.state()['customers'] if c['id']==cid)['tier']=='GOLD','Tự nâng Gold từ chi tiêu thực tế đạt 10 triệu')
    branch=admin.action('branch.save',name='Chi nhánh kiểm thử',address='Địa chỉ thử nghiệm')['id']
    room=admin.action('room.save',name='Phòng thử nghiệm',branch_id=branch,room_type='SPA',is_active=True)['id']
    admin.action('room.save',id=room,name='Phòng đã sửa',branch_id=branch,room_type='SPA',is_active=False)
    admin.action('room.save',expected=400,id=1,name='Spa 01',branch_id=1,room_type='SPA',is_active=False)
    admin.action('customer.active',expected=400,id=cid,is_active=False)
    spare=desk.action('customer.save',full_name='Hồ sơ tạm',phone='0907777777')['id']
    admin.action('customer.active',id=spare,is_active=False)
    check(next(c for c in admin.state()['customers'] if c['id']==spare)['is_active']==False,'Ngừng hồ sơ vẫn giữ lịch sử')
    admin.action('customer.active',id=spare,is_active=True)
    admin.action('staff.save',expected=400,id=staff,full_name='KTV mới',phone='0908888888',branch_id=1,commission_percent=15,is_active=False,service_ids=[s])
    receptionist=admin.action('account.save',username='reception2',password='Reception123',full_name='Lễ tân mới',role='RECEPTIONIST',is_active=True)['id']
    second=Client(base);second.login('reception2','Reception123')
    admin.action('account.save',id=receptionist,full_name='Lễ tân mới',role='RECEPTIONIST',is_active=False)
    second.request('/api/state',expected=401)
    desk.action('appointment.reschedule',expected=400,id=a,start_at='not-a-date')
    customer.request('/api/backup',expected=403)
    import io,zipfile
    backup=admin.opener.open(base+'/api/backup',timeout=20)
    check(backup.code==200,'Admin tải được bản sao lưu H2 nhất quán')
    backup_bytes=backup.read()
    admin.backup_bytes=backup_bytes
    with zipfile.ZipFile(io.BytesIO(backup_bytes)) as archive:
        check(any(name.endswith('.mv.db') for name in archive.namelist()) and archive.testzip() is None,'ZIP sao lưu có database hợp lệ')
    cookies='; '.join(cookie.name+'='+cookie.value for cookie in admin.cookies)
    def read_state(_):
        started=time.monotonic()
        request=urllib.request.Request(base+'/api/state',headers={'Cookie':cookies})
        with urllib.request.urlopen(request,timeout=20) as response:
            response.read()
            return response.code,time.monotonic()-started
    with concurrent.futures.ThreadPoolExecutor(50) as pool:
        results=list(pool.map(read_state,range(50)))
    check(all(code==200 for code,_ in results),'50 yêu cầu đọc trạng thái đồng thời thành công')
    times=sorted(elapsed for _,elapsed in results)
    print(f'PERF 50 concurrent reads: p95={times[47]:.3f}s max={times[-1]:.3f}s',flush=True)
    # Lockout persists across restart (verified again by main).
    lock=Client(base)
    for i in range(5): lock.request('/api/login',{'username':'ql_hoa','password':'wrong'},expected=423 if i==4 else 401)
    lock.request('/api/login',{'username':'ql_hoa','password':'123456'},expected=423)
    admin.action('account.save',id=2,role='MANAGER',full_name='Quản lý Hoa',is_active=True,password='')
    lock.login('ql_hoa')
    newcomer.action('password.change',old_password='SafePass123',new_password='ChangedPass123')
    newcomer.request('/api/logout',{})
    newcomer.request('/api/state',expected=401)
    newcomer.login('newcustomer','ChangedPass123')
    admin.action('account.save',expected=400,id=1,role='ADMIN',full_name='Admin',is_active=False)
    # Concurrent requests use one transaction lock: exactly one wins.
    left,right=Client(base),Client(base);left.login('letan01');right.login('letan01')
    payload={'action':'appointment.create','service_ids':[1],'branch_id':2,'staff_id':4,'customer_id':cid,'start_at':tomorrow+'T17:00'}
    def race(client):
        req=urllib.request.Request(base+'/api/action',data=json.dumps(payload).encode(),headers={'Content-Type':'application/json','X-CSRF-Token':client.csrf})
        try: return client.opener.open(req,timeout=20).code
        except urllib.error.HTTPError as e: return e.code
    with concurrent.futures.ThreadPoolExecutor(2) as pool:
        codes=list(pool.map(race,[left,right]))
    check(sorted(codes)==[200,400],'Hai yêu cầu đồng thời không tạo trùng lịch')
    return admin

if __name__=='__main__':
    # Make local test time around 08:xx so same-day lifecycle tests are repeatable.
    offset=8-dt.datetime.now(dt.timezone.utc).hour
    zone=f'GMT{offset:+03}:00'
    day=dt.datetime.now(dt.timezone(dt.timedelta(hours=offset))).date().isoformat()
    with tempfile.TemporaryDirectory(prefix='spacare-test-') as tmp:
        db=Path(tmp)/'db'
        server=Server(db,zone)
        try:
            admin=run(server.base,day)
            # Keep a locked account and data for restart verification.
            lock=Client(server.base)
            for i in range(5): lock.request('/api/login',{'username':'ql_hoa','password':'wrong'},expected=423 if i==4 else 401)
        finally:
            logs=server.stop()
            Path('/tmp/spacare-api-test-server.log').write_text(logs)
        # Simulate passage of 13 months for the isolated test database only.
        cp=str(ROOT/'target/classes')+os.pathsep+str(ROOT/'lib/h2-2.2.224.jar')
        fixture=subprocess.run(['java','-cp',cp,'org.h2.tools.Shell','-url','jdbc:h2:file:'+str(db),'-user','sa','-password','','-sql',"UPDATE invoice SET created_at=DATEADD('MONTH',-13,CURRENT_TIMESTAMP); UPDATE loyalty_entry SET expires_at=DATEADD('DAY',-1,CURRENT_TIMESTAMP) WHERE remaining_points>0"],capture_output=True,text=True)
        check(fixture.returncode==0 and 'Error' not in fixture.stdout,'Chuẩn bị lịch sử quá 12 tháng trong DB kiểm thử')
        server=Server(db,zone)
        try:
            client=Client(server.base);client.login('admin')
            check(len(client.state()['appointments'])>0,'Lịch, hóa đơn và kho tồn tại sau khởi động lại')
            Client(server.base).request('/api/login',{'username':'ql_hoa','password':'123456'},expected=423)
            check(True,'Khóa tài khoản giữ qua khởi động lại')
            state=client.state()
            check(all(c['spending_12m']==0 and c['tier']=='SILVER' for c in state['customers']),'Hạng tính đúng cửa sổ chi tiêu 12 tháng')
            check(all(c['point_balance']==0 for c in state['customers']),'Điểm quá hạn không được sử dụng')
        finally:
            server.stop()
        # Restore the actual downloaded snapshot into another isolated DB and boot it.
        backup_file=Path(tmp)/'downloaded-backup.zip';backup_file.write_bytes(admin.backup_bytes)
        restored_dir=Path(tmp)/'restored';restored_dir.mkdir()
        restore=subprocess.run(['java','-cp',cp,'org.h2.tools.Restore','-file',str(backup_file),'-dir',str(restored_dir),'-db','spacare'],capture_output=True,text=True)
        check(restore.returncode==0 and (restored_dir/'spacare.mv.db').exists(),'Khôi phục được ZIP bằng công cụ H2')
        server=Server(restored_dir/'spacare',zone)
        try:
            restored=Client(server.base);restored.login('admin');state=restored.state()
            check(len(state['invoices'])>=5 and len(state['appointments'])>0,'Bản khôi phục chạy lại với hóa đơn và lịch sử nguyên vẹn')
        finally:
            server.stop()
        keystore=Path(tmp)/'test.p12';certificate=Path(tmp)/'test.pem'
        tls_env={**os.environ,'SPACARE_TEST_CERT_PASSWORD':'TestTls12345'}
        generated=subprocess.run(['keytool','-genkeypair','-alias','spacare','-storetype','PKCS12','-keystore',str(keystore),'-storepass:env','SPACARE_TEST_CERT_PASSWORD','-keypass:env','SPACARE_TEST_CERT_PASSWORD','-keyalg','RSA','-keysize','2048','-validity','2','-dname','CN=localhost','-ext','SAN=DNS:localhost,IP:127.0.0.1','-noprompt'],env=tls_env,capture_output=True,text=True)
        check(generated.returncode==0,'Tạo chứng chỉ thử nghiệm PKCS12')
        exported=subprocess.run(['keytool','-exportcert','-rfc','-alias','spacare','-keystore',str(keystore),'-storepass:env','SPACARE_TEST_CERT_PASSWORD','-file',str(certificate)],env=tls_env,capture_output=True,text=True)
        check(exported.returncode==0,'Xuất chứng chỉ để xác thực TLS trong kiểm thử')
        server=Server(db,zone,tls=keystore)
        try:
            context=ssl.create_default_context(cafile=str(certificate))
            client=Client(server.base,context);client.login('admin')
            check(all(cookie.secure for cookie in client.cookies),'Phiên HTTPS đặt cookie Secure')
            check(client.state()['user']['role']=='ADMIN','HTTPS chạy API với xác thực chứng chỉ hợp lệ')
        finally:
            server.stop()
    print(f'PASS {CHECKS} acceptance checks')
