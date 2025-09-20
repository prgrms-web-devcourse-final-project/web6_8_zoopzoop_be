output "vpc_id" {
  value       = aws_vpc.this.id
}

output "subnet_ids" {
  value       = [
    aws_subnet.a.id,
    aws_subnet.b.id,
    aws_subnet.c.id,
    aws_subnet.d.id
  ]
}