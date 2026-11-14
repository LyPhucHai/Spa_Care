#!/bin/sh
set -eu
cd "$(dirname "$0")"
sh build.sh
exec java -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Ho_Chi_Minh -cp 'target/classes:lib/h2-2.2.224.jar' spacare.web.SpaCareServer
