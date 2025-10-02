variable "ami" { type = string }
variable "ec2_instance_type" { type = string }
variable "subnet_id" { type = string }
variable "ec2_sg_id" { type = string }
variable "iam_instance_profile" { type = string }
variable "key_name" { type = string }
variable "prefix" { type = string }
# variable "redis_password" { type = string }
variable "test_mysql_root_password" {
  type = string
  default = null
}
variable "test_mysql_db_name" {
  type=string
  default = null
}
variable "create_rds" {
  type = bool
  default = false
}