variable "identifier" {}
variable "engine" {}
variable "engine_version" {}
variable "rds_instance_class" {}
variable "allocated_storage" {}
variable "storage_type" {}
variable "prod_mysql_db_username" {}
variable "prod_mysql_root_password" {}
variable "prod_mysql_db_name" {}
variable "vpc_security_group_ids" { type = list(string) }
variable "subnet_ids" { type = list(string) }
variable "multi_az" { type = bool }
variable "tags" { type = map(string) }
variable "skip_final_snapshot" { type = bool }