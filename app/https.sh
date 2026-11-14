#!/bin/sh
set -eu
cd "$(dirname "$0")"
umask 077
mkdir -p certs
if [ ! -f certs/development.p12 ]; then
    command -v keytool >/dev/null 2>&1 || { echo 'Cần keytool của JDK 17+.' >&2; exit 1; }
    SPACARE_TLS_PASSWORD=$(head -c 32 /dev/urandom | base64)
    export SPACARE_TLS_PASSWORD
    keytool -genkeypair -alias spacare -storetype PKCS12 -keystore certs/development.p12 -storepass:env SPACARE_TLS_PASSWORD -keypass:env SPACARE_TLS_PASSWORD -keyalg RSA -keysize 2048 -validity 365 -dname 'CN=localhost,OU=Coursework,O=SpaCare,L=Ho Chi Minh,C=VN' -ext 'SAN=DNS:localhost,IP:127.0.0.1' -noprompt
    printf '%s\n' "$SPACARE_TLS_PASSWORD" > certs/password.txt
fi
IFS= read -r SPACARE_TLS_PASSWORD < certs/password.txt
export SPACARE_TLS_PASSWORD
export SPACARE_TLS_KEYSTORE=./certs/development.p12
export PORT=${PORT:-8443}
echo 'HTTPS demo dùng chứng chỉ tự ký. Mở https://localhost:'"$PORT"
exec sh run.sh
