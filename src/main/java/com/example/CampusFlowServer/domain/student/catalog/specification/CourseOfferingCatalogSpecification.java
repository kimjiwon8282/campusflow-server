package com.example.CampusFlowServer.domain.student.catalog.specification;

import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class CourseOfferingCatalogSpecification {

    private CourseOfferingCatalogSpecification() {
    }

    public static Specification<CourseOffering> byCondition(
        Integer year,
        SemesterTerm term,
        String collegeName,
        String departmentName,
        String category
    ) {
        return base(year, term)
            .and(equalsIgnoreCase("subject.department.collegeName", collegeName))
            .and(equalsIgnoreCase("subject.department.name", departmentName))
            .and(equalsIgnoreCase("subject.category", category)); //동적으로 만들어진 where절
    }

    public static Specification<CourseOffering> byDirect(
        Integer year,
        SemesterTerm term,
        String subjectName,
        String professorName,
        String departmentName
    ) {
        return base(year, term)
            .and(likeIgnoreCase("subject.name", subjectName))
            .and(likeIgnoreCase("professor.member.name", professorName))
            .and(likeIgnoreCase("subject.department.name", departmentName));
    }

    private static Specification<CourseOffering> base(Integer year, SemesterTerm term) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.join("semester", JoinType.INNER).get("year"), year),
                criteriaBuilder.equal(root.join("semester", JoinType.INNER).get("term"), term)
            );
        };
    }

    private static Specification<CourseOffering> equalsIgnoreCase(String path, String value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                criteriaBuilder.upper(resolve(root, path).as(String.class)),
                value.toUpperCase()
            );
        };
    }

    private static Specification<CourseOffering> likeIgnoreCase(String path, String value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.upper(resolve(root, path).as(String.class)),
                "%" + value.toUpperCase() + "%"
            );
        };
    }

    private static jakarta.persistence.criteria.Path<?> resolve(
        jakarta.persistence.criteria.Root<CourseOffering> root,
        String path
    ) {
        String[] parts = path.split("\\.");
        jakarta.persistence.criteria.From<?, ?> from = root;
        for (int i = 0; i < parts.length - 1; i++) {
            from = from.join(parts[i], JoinType.INNER);
        }
        return from.get(parts[parts.length - 1]);
    }
}
