#공통 변수 정의

#EC2
variable "region" { type = string }
variable "prefix" { type = string }
variable "ami" { type = string }
variable "ec2_instance_type" { type = string }
variable "key_name" { type = string }
#variable "redis_password" { type = string }
variable "test_mysql_root_password" {
  type = string
  default = null
}
variable "test_mysql_db_name" {
  type = string
  default = null
}


#RDS
variable "create_rds" {
  description = "RDS 생성 여부"
  type        = bool
}
variable "identifier" {
  type = string
 default = null
}
variable "engine" {
  type = string
  default = null
}
variable "engine_version" {
  type = string
  default = null
}
variable "rds_instance_type" {
  type = string
  default = null
}
variable "allocated_storage" {
  type = number
  default = null
}
variable "storage_type" {
  type = string
  default = null
}
variable "multi_az" {
  type = bool
  default = null
}
variable "prod_mysql_db_username" {
  type = string
  default = null
}
variable "prod_mysql_root_password" {
  type = string
  default = null
}
variable "prod_mysql_db_name" {
  type = string
  default = null
}
variable "skip_final_snapshot" {
  type = string
  default = null
}