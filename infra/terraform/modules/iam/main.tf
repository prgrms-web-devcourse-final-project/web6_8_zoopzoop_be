# EC2 역할 생성
resource "aws_iam_role" "ec2_role" {
  name = "${var.prefix}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action = "sts:AssumeRole"
    }]
  })
}

# 역할에 S3 접근 정책 부착 (사용하지 않을 경우 주석 처리)
# resource "aws_iam_role_policy_attachment" "s3_full" {
#   role       = aws_iam_role.ec2_role.name
#   policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
# }

# 역할에 SSM 접근 정책 부착 (AWS Systems Manager)
resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
}

# EC2에서 역할을 사용할 수 있게 인스턴스 프로파일 생성
resource "aws_iam_instance_profile" "this" {
  name = "${var.prefix}-instance-profile"
  role = aws_iam_role.ec2_role.name
}
