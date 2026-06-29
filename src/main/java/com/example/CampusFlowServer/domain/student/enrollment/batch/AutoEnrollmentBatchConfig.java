package com.example.CampusFlowServer.domain.student.enrollment.batch;

import com.example.CampusFlowServer.domain.enrollment.enums.WishAutoApplyResult;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class AutoEnrollmentBatchConfig {

    public static final String JOB_NAME = "autoEnrollmentPreApplyJob"; //희망과목 자동신청 사전 반영 작업 전체
    public static final String STEP_NAME = "applyDoneWishCoursesStep"; //DONE 희망과목을 읽고 자동신청으로 반영하는 단계
    private static final int CHUNK_SIZE = 20;

    @Bean(JOB_NAME)
    public Job autoEnrollmentPreApplyJob(
        JobRepository jobRepository,
        @Qualifier(STEP_NAME) Step applyDoneWishCoursesStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(applyDoneWishCoursesStep)//하나의 스텝으로 구성
            .build();
    }

    @Bean(STEP_NAME)
    public Step applyDoneWishCoursesStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        JpaPagingItemReader<AutoEnrollmentBatchItem> autoEnrollmentBatchReader,
        AutoEnrollmentBatchWriter autoEnrollmentBatchWriter
    ) {
        return new StepBuilder(STEP_NAME, jobRepository)
            .<AutoEnrollmentBatchItem, AutoEnrollmentBatchItem>chunk(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(autoEnrollmentBatchReader) //자동신청 대상 조회
            .writer(autoEnrollmentBatchWriter) //각 대상 처리
            .listener(autoEnrollmentBatchWriter) //처리결과 집계 저장
            .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<AutoEnrollmentBatchItem> autoEnrollmentBatchReader(
        EntityManagerFactory entityManagerFactory,
        @Value("#{jobParameters['semesterId']}") Long semesterId
    ) {
        return new JpaPagingItemReaderBuilder<AutoEnrollmentBatchItem>()
            .name("autoEnrollmentBatchReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("""
                select new com.example.CampusFlowServer.domain.student.enrollment.batch.AutoEnrollmentBatchItem(
                    w.id,
                    w.courseOffering.id
                )
                from WishCourse w
                where w.semester.id = :semesterId
                  and w.autoApply = true
                  and w.result = :result
                order by w.id asc
                """)
            .parameterValues(Map.of(
                "semesterId", semesterId,
                "result", WishAutoApplyResult.DONE
            ))
            .pageSize(CHUNK_SIZE)
            .saveState(true)
            .build();
    }
}
