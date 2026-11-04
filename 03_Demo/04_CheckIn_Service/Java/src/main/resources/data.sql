INSERT INTO membership_tier VALUES (1,'SILVER',0,1.00,0),(2,'GOLD',10000000,1.20,5),(3,'PLATINUM',30000000,1.50,10);
INSERT INTO customer VALUES (1,2,'Nguyễn Thu H','0903000123',420,18000000),(2,1,'Trần Mai','0912000456',80,9500000),(3,3,'Lê Linh','0987000789',900,35000000);
INSERT INTO staff VALUES (1,'Lan'),(2,'Hà'),(3,'Minh');
INSERT INTO service VALUES (1,'Massage body 90p',500000,90,TRUE),(2,'Gội đầu thư giãn 60p',150000,60,TRUE),(3,'Chăm sóc da cơ bản 60p',400000,60,TRUE),(4,'Cắt tóc nữ 45p',200000,45,TRUE),(5,'Sơn gel 60p',250000,60,TRUE);
INSERT INTO staff_service VALUES (1,1),(1,2),(1,3),(2,3),(2,5),(3,2),(3,4);
INSERT INTO room VALUES (1,'Phòng 1'),(2,'Phòng 2'),(3,'Khu nail'),(4,'Khu tóc');
INSERT INTO shift VALUES (1,'Sáng','08:00:00','14:00:00'),(2,'Chiều','14:00:00','20:00:00'),(3,'Cả ngày','08:00:00','20:00:00');
