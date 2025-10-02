output "ec2_sg_id" {
    value = aws_security_group.ec2_sg.id
}

output "rds_sg_id" {
    value = var.create_rds?aws_security_group.rds_sg[0].id:null
}