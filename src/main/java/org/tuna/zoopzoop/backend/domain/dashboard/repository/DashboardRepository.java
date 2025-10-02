package org.tuna.zoopzoop.backend.domain.dashboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Dashboard;

@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, Integer> {
}
