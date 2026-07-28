package datart.server.job;

import datart.core.base.consts.Const;
import datart.core.common.Application;
import datart.core.entity.Source;
import datart.server.service.SourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchemaSyncJobTest {

    private ApplicationContext originalContext;
    private SourceService sourceService;
    private Scheduler scheduler;
    private JobExecutionContext executionContext;
    private JobKey jobKey;

    @BeforeEach
    void setUp() {
        originalContext = Application.getContext();
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        sourceService = mock(SourceService.class);
        scheduler = mock(Scheduler.class);
        when(applicationContext.getBean(SourceService.class)).thenReturn(sourceService);
        when(applicationContext.getBean(Scheduler.class)).thenReturn(scheduler);
        new Application().setApplicationContext(applicationContext);

        executionContext = mock(JobExecutionContext.class);
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(SchemaSyncJob.SOURCE_ID, "source-1");
        when(executionContext.getMergedJobDataMap()).thenReturn(dataMap);
        JobDetail jobDetail = mock(JobDetail.class);
        jobKey = JobKey.jobKey("schema-sync-source-1");
        when(jobDetail.getKey()).thenReturn(jobKey);
        when(executionContext.getJobDetail()).thenReturn(jobDetail);
    }

    @AfterEach
    void restoreApplicationContext() {
        new Application().setApplicationContext(originalContext);
    }

    @Test
    void deletesJobWhenSourceNoLongerExists() throws Exception {
        when(sourceService.retrieve("source-1", false)).thenReturn(null);
        SchemaSyncJob job = spy(new SchemaSyncJob());

        job.execute(executionContext);

        verify(scheduler).deleteJob(jobKey);
        verify(job, never()).execute("source-1");
    }

    @Test
    void deletesJobWhenSourceIsArchived() throws Exception {
        when(sourceService.retrieve("source-1", false)).thenReturn(source(Const.DATA_STATUS_ARCHIVED));
        SchemaSyncJob job = spy(new SchemaSyncJob());

        job.execute(executionContext);

        verify(scheduler).deleteJob(jobKey);
        verify(job, never()).execute("source-1");
    }

    @Test
    void keepsJobAndSynchronizesActiveSource() throws Exception {
        when(sourceService.retrieve("source-1", false)).thenReturn(source(Const.DATA_STATUS_ACTIVE));
        SchemaSyncJob job = spy(new SchemaSyncJob());
        doReturn(true).when(job).execute("source-1");

        job.execute(executionContext);

        verify(scheduler, never()).deleteJob(jobKey);
        verify(job).execute("source-1");
    }

    private Source source(byte status) {
        Source source = new Source();
        source.setId("source-1");
        source.setStatus(status);
        return source;
    }
}
