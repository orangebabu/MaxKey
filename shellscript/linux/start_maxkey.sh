#!/bin/bash
source ./set_maxkey_env.sh

JAVA_MARK=MaxKeyBoot
JAVA_OPTS=" -Xms256m "
JAVA_OPTS="${JAVA_OPTS} -Xmx2048m"
JAVA_OPTS="${JAVA_OPTS} -Dfile.encoding=UTF-8"
JAVA_OPTS="${JAVA_OPTS} -DjavaMark=${JAVA_MARK}"

JAVA_CONF=./maxkey
JAVA_LIBMAXKEYPATH=./maxkey
JAVA_CLASSPATH=./classes:./bin:$JAVA_CONF:
JAVA_MAINCLASS=org.maxkey.MaxKeyApplication
JAVA_EXEC=$JAVA_HOME/bin/java

export JAVA_CLASSPATH
export JAVA_LIBPATH
export JAVA_LIBMAXKEYPATH


for LL in `ls $JAVA_LIBMAXKEYPATH/*.jar`
        do
                JAVA_CLASSPATH=$LL
               
done

export JAVA_CLASSPATH
# Display our environment
echo "-------------------------------------------------------------------------------"
echo "  Bootstrap Environment"
echo ""
echo JAVA_CLASSPATH :  ${JAVA_CLASSPATH}
echo JAVA_CONF      :  $JAVA_CONF
echo JAVA_OPTS      :  $JAVA_OPTS
echo JAVA_HOME      :  $JAVA_HOME  
echo JAVA           :  $JAVA_EXEC
${JAVA_EXEC} -version
echo ""
echo "-------------------------------------------------------------------------------"
echo ""

nohup $JAVA_EXEC -jar $JAVA_OPTS $JAVA_CLASSPATH >./logs/nohup_maxkey.out 2>&1 &

echo MaxKey is starting，you can check the ./logs/nohup_maxkey.out
