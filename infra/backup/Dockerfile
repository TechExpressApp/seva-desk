FROM alpine:3.22
RUN apk add --no-cache postgresql16-client age rclone curl tzdata
COPY backup.sh restore.sh /usr/local/bin/
COPY crontab /etc/crontabs/root
RUN chmod +x /usr/local/bin/backup.sh /usr/local/bin/restore.sh
CMD ["crond", "-f", "-l", "2"]
