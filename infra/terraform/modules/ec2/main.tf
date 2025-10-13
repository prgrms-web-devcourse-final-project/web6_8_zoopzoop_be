locals {
  mysql_user_data= var.create_rds?"": <<-END1
  # MySQL 컨테이너 실행 (테스트 환경일 때만)
docker run -d --name mysql \
  --restart unless-stopped \
  --network common \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=${var.test_mysql_root_password} \
  -e MYSQL_DATABASE=${var.test_mysql_db_name} \
  -v /opt/mysql/data:/var/lib/mysql \
   mysql:latest
END1

  user_data = <<-END2
#!/bin/bash
# Swap 설정
dd if=/dev/zero of=/swapfile bs=128M count=32
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo "/swapfile swap swap defaults 0 0" >> /etc/fstab

# Docker 설치
yum install -y docker
systemctl enable docker
systemctl start docker
docker network create common

# Nginx, Redis, MySQL 컨테이너 실행 (테스트용)
docker run -d --name npm \
  --restart unless-stopped \
  --network common \
  -p 80:80 -p 443:443 -p 81:81 \
  -v /opt/npm/data:/data \
  -v /opt/npm/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest
# docker run -d --name redis_1 --restart unless-stopped --network common -p 6379:6379 -e TZ=Asia/Seoul redis

${local.mysql_user_data}
END2
}

resource "aws_instance" "this" {
  ami                    = var.ami
  instance_type          = var.ec2_instance_type
  subnet_id              = var.subnet_id
  vpc_security_group_ids = [var.ec2_sg_id]
  iam_instance_profile   = var.iam_instance_profile
  associate_public_ip_address = true
  key_name               = var.key_name

  root_block_device {
    volume_type = "gp3"
    volume_size = 25
    tags = { Name = "${var.prefix}-ebs" }
  }

  user_data = local.user_data

  tags = { Name = "${var.prefix}-ec2" }
}
