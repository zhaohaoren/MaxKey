echo "network create "

docker network create maxkey.top

mysql_version=8.4.2
#MySQL
docker pull mysql:$mysql_version
docker image tag mysql:$mysql_version maxkeytop/mysql

#maxkey
docker pull maxkeytop/maxkey:latest

#maxkey-mgt
docker pull maxkeytop/maxkey-mgt:latest

#maxkey-frontend，构建统一 Vue 前端镜像
docker build -f ../../maxkey-web-frontend/maxkey-web-vue-app/Dockerfile -t maxkeytop/maxkey-frontend:latest ../../maxkey-web-frontend/maxkey-web-vue-app

#maxkey-nginx proxy
cd docker-nginx

docker build -f Dockerfile -t maxkeytop/maxkey-nginx .

cd ..

echo "installed done."
