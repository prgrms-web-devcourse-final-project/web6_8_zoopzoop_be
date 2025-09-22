resource "aws_db_instance" "this" {
  identifier = var.identifier
  engine = var.engine
  engine_version = var.engine_version
  instance_class = var.rds_instance_class
  allocated_storage = var.allocated_storage
  storage_type = var.storage_type
  username = var.prod_mysql_db_username
  password = var.prod_mysql_root_password
  db_name = var.prod_mysql_db_name
  vpc_security_group_ids = var.vpc_security_group_ids
  db_subnet_group_name = aws_db_subnet_group.this.name
  skip_final_snapshot = var.skip_final_snapshot
  multi_az = var.multi_az
  tags = var.tags
}

resource "aws_db_subnet_group" "this" {
  name       = "${var.identifier}-db-subnet-group"
  subnet_ids = var.subnet_ids
  tags = var.tags
}