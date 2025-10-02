resource "aws_security_group" "ec2_sg" {
  name   = "${var.prefix}-ec2-sg"
  vpc_id = var.vpc_id

  description = "EC2 security group"

  ingress {
    description = "Allow HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Allow HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Allow SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["221.154.167.13/32"]
  }

  ingress{
    from_port = 81
    to_port = 81
    protocol = "tcp"
    cidr_blocks = ["221.154.167.13/32"]
  }

  # 개발시에만 열어놓고, 운영시에는 닫기
  ingress{
    from_port = 8080
    to_port = 8080
    protocol = "tcp"
    cidr_blocks = ["221.154.167.13/32"]
  }

  # 개발시에만 열어놓고, 운영시에는 닫기
  ingress{
    from_port = 3306
    to_port = 3306
    protocol = "tcp"
    cidr_blocks = ["221.154.167.13/32"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.prefix}-ec2-sg"
  }
}
