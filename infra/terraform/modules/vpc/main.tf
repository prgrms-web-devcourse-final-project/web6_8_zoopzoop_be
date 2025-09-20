
resource "aws_vpc" "this"{
  cidr_block = "10.0.0.0/16"
  enable_dns_support = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.prefix}-vpc"
  }
}

resource "aws_subnet" "a"{
  vpc_id = aws_vpc.this.id
  cidr_block = "10.0.1.0/24"
  availability_zone = "${var.region}a"
  map_public_ip_on_launch = true
  tags = {Name = "${var.prefix}-subnet-a"}
}

resource "aws_subnet" "b"{
  vpc_id = aws_vpc.this.id
  cidr_block = "10.0.2.0/24"
  availability_zone = "${var.region}b"
  map_public_ip_on_launch = true
  tags = {Name = "${var.prefix}-subnet-b"}
}

resource "aws_subnet" "c"{
  vpc_id = aws_vpc.this.id
  cidr_block = "10.0.3.0/24"
  availability_zone = "${var.region}c"
  map_public_ip_on_launch = true
  tags = {Name = "${var.prefix}-subnet-c"}
}

resource "aws_subnet" "d"{
  vpc_id = aws_vpc.this.id
  cidr_block = "10.0.4.0/24"
  availability_zone = "${var.region}d"
  map_public_ip_on_launch = true
  tags = {Name = "${var.prefix}-subnet-d"}
}

resource "aws_internet_gateway" "this"{
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "${var.prefix}-igw"
  }
}

resource "aws_route_table" "this" {
  vpc_id = aws_vpc.this.id

  route{
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = {
    Name = "${var.prefix}-public-rt"
  }
}

resource "aws_route_table_association" "a" {
  subnet_id = aws_subnet.a.id
  route_table_id = aws_route_table.this.id
}

resource "aws_route_table_association" "b" {
  subnet_id = aws_subnet.b.id
  route_table_id = aws_route_table.this.id
}

resource "aws_route_table_association" "c" {
  subnet_id = aws_subnet.c.id
  route_table_id = aws_route_table.this.id
}

resource "aws_route_table_association" "d" {
  subnet_id = aws_subnet.d.id
  route_table_id = aws_route_table.this.id
}
