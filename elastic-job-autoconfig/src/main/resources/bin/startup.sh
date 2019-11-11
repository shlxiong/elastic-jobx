DEPLOY_DIR=`pwd`
LIB_DIR=${DEPLOY_DIR}/lib/*
echo $LIB_DIR
CONSOLE_MAIN=cn.sumpay.config.autotect.elasticjob.test.ElasticJobTest

java -classpath ../${LIB_DIR}:. ${CONSOLE_MAIN}