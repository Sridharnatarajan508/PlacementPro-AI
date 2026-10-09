@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __ MVNW_CMD__=%MAVEN_PROJECTBASEDIR%\mvnw.cmd
@SET MAVEN_PROJECTBASEDIR=%~dp0

@SET MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
@SET MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_WRAPPER_PROPERTIES%") DO (
    @IF "%%A"=="distributionUrl" SET DISTRIBUTION_URL=%%B
)

@SET JAVA_HOME_TRIMMED=%JAVA_HOME:"=%
@IF NOT "%JAVA_HOME_TRIMMED%"=="" (
    @SET _JAVA_CMD=%JAVA_HOME_TRIMMED%\bin\java.exe
) ELSE (
    @SET _JAVA_CMD=java
)

@"%_JAVA_CMD%" -jar "%MAVEN_WRAPPER_JAR%" %*
