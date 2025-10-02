output "vpc_id" {
  value       = aws_vpc.this.id
}

output "subnet_ids" {
  value       = [
    aws_subnet.public.id,
    aws_subnet.private.id,
    aws_subnet.private2.id
  ]
}

output "private_subnet_ids"{
  value = [
    aws_subnet.private.id,
    aws_subnet.private2.id
  ]
}