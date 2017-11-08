FROM quay.io/alfresco/alfresco-base-java:0.1
VOLUME /tmp
ARG final_name
COPY $final_name $final_name
# env variable for entrypoint
ENV final_name $final_name
EXPOSE 8080
ENTRYPOINT java -jar $final_name
