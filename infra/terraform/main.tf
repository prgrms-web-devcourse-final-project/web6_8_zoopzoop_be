terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

provider "aws" {
  region = var.aws_region
}


module "ec2" {
  source        = "./modules/ec2"
  instance_name = var.instance_name
  ami           = var.ami_id
  instance_type = var.instance_type
  key_name      = var.key_name
}