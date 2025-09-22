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
}

module "iam"{
  source = "./modules/iam"
  prefix = var.prefix
}

module "ec2" {
  source        = "./modules/ec2"
  ami           = var.ami
  instance_type = var.instance_type
  subnet_id     = module.vpc.subnet_ids[0]
  ec2_sg_id       = module.sg.ec2_sg_id
  iam_instance_profile = module.iam.instance_profile_name
  key_name      = var.key_name
  prefix       = var.prefix
  mysql_root_password = var.mysql_root_password
  mysql_db_name = var.mysql_db_name
}

