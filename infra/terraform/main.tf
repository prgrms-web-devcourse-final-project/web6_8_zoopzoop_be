#루트에서 모듈 호출
terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

provider "aws" {
  region = var.region
}


module "vpc" {
  source             = "./modules/vpc"
  prefix             = var.prefix
  region             = var.region
}

module "sg"{
  source   = "./modules/sg"
  vpc_id  = module.vpc.vpc_id
  prefix  = var.prefix
  create_rds = var.create_rds
}

module "iam"{
  source = "./modules/iam"
  prefix = var.prefix
}

module "ec2" {
  source        = "./modules/ec2"
  ami           = var.ami
  ec2_instance_type = var.ec2_instance_type
  subnet_id     = module.vpc.subnet_ids[0]
  ec2_sg_id       = module.sg.ec2_sg_id
  iam_instance_profile = module.iam.instance_profile_name
  key_name      = var.key_name
  prefix       = var.prefix
  test_mysql_root_password = var.test_mysql_root_password
  test_mysql_db_name = var.test_mysql_db_name
  create_rds = var.create_rds
}

module "rds" {
  source = "./modules/rds"

  count = var.create_rds ? 1 : 0

  identifier = var.identifier
  engine = var.engine
  engine_version = var.engine_version
  rds_instance_class = var.rds_instance_type
  allocated_storage = var.allocated_storage
  storage_type = var.storage_type
  prod_mysql_db_username = var.prod_mysql_db_username
  prod_mysql_root_password = var.prod_mysql_root_password
  prod_mysql_db_name = var.prod_mysql_db_name
  vpc_security_group_ids = [module.sg.rds_sg_id]
  subnet_ids = module.vpc.subnet_ids
  multi_az = var.multi_az
    skip_final_snapshot = var.skip_final_snapshot
  tags = {
    Name = "${var.prefix}-rds"
  }
}
