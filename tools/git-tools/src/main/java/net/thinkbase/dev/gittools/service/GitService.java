package net.thinkbase.dev.gittools.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.liaochong.myexcel.core.Csv;
import com.github.liaochong.myexcel.core.CsvBuilder;
import com.github.liaochong.myexcel.core.DefaultStreamExcelBuilder;
import com.github.liaochong.myexcel.utils.FileExportUtil;

import net.thinkbase.dev.gittools.config.GittoolsConfig;
import net.thinkbase.dev.gittools.service.utils.GitUtils;
import net.thinkbase.dev.gittools.service.vo.ExportParameters;
import net.thinkbase.dev.gittools.service.vo.ExportResult;
import net.thinkbase.dev.gittools.xls.vo.StatDetailBean;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/service")
public class GitService {
	private static final Logger logger =  LoggerFactory.getLogger(GitService.class);
	
	@Autowired
	@Qualifier(GittoolsConfig.EXPORT_WORK_DIR)
	private File exportWorkDir;

	@PostMapping("/export-stat/{fileType}/{detailLevel}")
	public Mono<ExportResult> doExportStatDetail(
			@RequestBody ExportParameters params,
			@PathVariable("fileType") String fileType,
			@PathVariable("detailLevel") String detailLevel) throws IOException, GitAPIException {
		
		Boolean export2Excel;
		if ("xlsx".equals(fileType)) {
			export2Excel = true;
		}else if("csv".equals(fileType)) {
			export2Excel = false;
		}else {
			throw new UnsupportedOperationException("无效的导出文件类型: "+ fileType);
		}
		
		Boolean summaryDiffEntries;
		if ("commit".equals(detailLevel)) {
			summaryDiffEntries = true;
		}else if ("file".equals(detailLevel)) {
			summaryDiffEntries = false;
		}else {
			throw new UnsupportedOperationException("无效的数据明细程度: "+ detailLevel);
		}
		
		String pathLines = params.getPathLines();
		if (StringUtils.isBlank(pathLines)) {
			throw new IllegalArgumentException("未指定 pathLines 参数");
		}
		String[] paths = pathLines.split("[\\n|\\r]");
		List<File> repos = GitUtils.findAllGitRepos(paths);
		
		if (export2Excel) {
			var data = exportExcel(repos, summaryDiffEntries, params.getExtScriptFile());
			return Mono.just(data);
		}else {
			var data = exportCsv(repos, summaryDiffEntries, params.getExtScriptFile());
			return Mono.just(data);
		}
	}

	private ExportResult exportExcel(List<File> repos, boolean summaryDiffEntries, String extScriptFile)
			throws IOException, GitAPIException {
		final int idxRepoCount=0, idxCommitCount=1;
		final var startTime=System.currentTimeMillis();
		var buf = new long[] {0, 0};
		
	    try(DefaultStreamExcelBuilder<StatDetailBean> excelBuilder = 
	    		DefaultStreamExcelBuilder.of(StatDetailBean.class).fixedTitles().start()){
		    
			for (var dir: repos) {
				FileRepositoryBuilder builder = new FileRepositoryBuilder();
				try (var repo = builder.setGitDir(dir).build()) {
					logger.info("Begin process repo {} ...", repo.getDirectory());
					buf[idxRepoCount]++;
					GitUtils.analyseRepoCommits(repo, (ci)->{
						
						excelBuilder.append(StatDetailBean.fromCommitStatInfo(ci));
						
						buf[idxCommitCount]++;
					}, summaryDiffEntries);
		        }
			}
			
		    Workbook workbook = excelBuilder.build();
		    String fileName = buildDataFileName(StatDetailBean.SHEET_NAME, ".xlsx");
		    FileExportUtil.export(workbook, new File(exportWorkDir, fileName));
			
			String msg = "导出操作完成: 共处理 "+buf[idxRepoCount]+ "个仓库, "+buf[idxCommitCount]+" 个提交数据,"
					+ " 耗时"+(System.currentTimeMillis()-startTime)/1000+"秒";
			logger.info(fileName+" - " + msg);
			
			var data = new ExportResult(msg);
			data.setDownloadAddress(fileName);
			return data;
	    }
	}
	private ExportResult exportCsv(List<File> repos, boolean summaryDiffEntries, String extScriptFile)
			throws IOException, GitAPIException {
		final int idxRepoCount=0, idxCommitCount=1;
		final var startTime=System.currentTimeMillis();
		var buf = new long[] {0, 0};
		
	    List<StatDetailBean> infos = new ArrayList<StatDetailBean>();
	    
		for (var dir: repos) {
			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			try (var repo = builder.setGitDir(dir).build()) {
				logger.info("Begin process repo {} ...", repo.getDirectory());
				buf[idxRepoCount]++;
				GitUtils.analyseRepoCommits(repo, (ci)->{
					
					StatDetailBean bean = StatDetailBean.fromCommitStatInfo(ci);
					makeCompatible4CSV(bean);
					infos.add(bean);
					
					buf[idxCommitCount]++;
				}, summaryDiffEntries);
	        }
		}
		
		try (CsvBuilder<StatDetailBean> csvBuilder = CsvBuilder.of(StatDetailBean.class)){
			Csv csv = csvBuilder.build(infos);
			
		    String fileName = buildDataFileName(StatDetailBean.SHEET_NAME, ".csv");
		    csv.write(new File(exportWorkDir, fileName).toPath());

			String msg = "导出操作完成: 共处理 "+buf[idxRepoCount]+ "个仓库, "+buf[idxCommitCount]+" 个提交数据,"
					+ " 耗时"+(System.currentTimeMillis()-startTime)/1000+"秒";
			logger.info(fileName+" - " + msg);
			
			var data = new ExportResult(msg);
			data.setDownloadAddress(fileName);
			return data;
		}
	}

	/**
	 * FIXME: Make fields(comment and diffs) compatible with CSV file format.
	 * see {@link CsvBuilder#PATTERN_QUOTES_PREMISE} for detail reason.
	 * @param bean
	 */
	private void makeCompatible4CSV(StatDetailBean bean) {
		String comment = bean.getComment()+" ,";
		bean.setComment(comment);
		String diffs = bean.getDiffs() + " ,";
		bean.setDiffs(diffs);
	}
	
	private String buildDataFileName(String sheetName, String postfix) {
		String timestamp = DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMdd-HHmmss");
		String fileName=sheetName+"."+timestamp+postfix;
		return fileName;
	}
}
