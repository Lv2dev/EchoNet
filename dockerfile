# Java 런타임을 포함하는 베이스 이미지로 시작
FROM amazoncorretto:17

# 유지보수자 정보 추가
LABEL maintainer="lv2dev@gmail.com"

# /tmp에 볼륨 추가
VOLUME /tmp

# 이 컨테이너 외부로 8099 포트를 사용 가능하게 함
EXPOSE 8099

# 애플리케이션의 jar 파일
ARG JAR_FILE=build/libs/*.jar

# 애플리케이션의 jar 파일을 컨테이너에 추가
ADD ${JAR_FILE} app.jar

# jar 파일 실행
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]