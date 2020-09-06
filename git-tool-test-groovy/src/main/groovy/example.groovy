import org.apache.commons.lang3.time.DateFormatUtils

import org.bizobj.gittools.service.vo.CommitStatInfo
import org.bizobj.gittools.xls.vo.StatDetailBean

/**
 * exOrganization;  //组织
 * exTeam;          //团队
 * exTeamLeader;    //组长
 * exProductLine;   //产品
 * exComponent;     //组件
 * exStage;         //阶段
 * exTask;          //任务
 * exValidation;    //有效性
 * exAttribute1;    //属性1
 * exAttribute2;    //属性2
 * exAttribute3;    //属性3
 * 
 * git clone https://github.com/vuejs/cn.vuejs.org.git
 * git clone https://github.com/bailicangdu/vue2-manage.git
 * git clone https://github.com/mozilla/rhino.git
 * git clone https://github.com/ehcache/ehcache3.git
 */
class GitToolsEx {

	def run(CommitStatInfo ci, StatDetailBean si) {
		
		if (ci.repo ==~ /.*vue.*/) {
			si.exOrganization = "vuejs";
		}else if (ci.repo ==~ /.*rhino.*/) {
			si.exOrganization = "Mozilla";
		}else if (ci.repo ==~ /.*ehcache.*/) {
			si.exOrganization = "EhCache";
		}
		
		if (ci.repo == "cn.vuejs.org") {
			si.exTeam = "中文";
			si.exTeamLeader = "张三";
		}
		
		if (ci.repo ==~ /.*vue.*/) {
			si.exProductLine = "Web";
		}else if (ci.repo ==~ /.*rhino.*/) {
			si.exProductLine = "Java";
		}else if (ci.repo ==~ /.*ehcache.*/) {
			si.exProductLine = "Java";
		}
		
		if (ci.repo ==~ /.*vue.*/) {
			si.exComponent = "MVVC";
		}else if (ci.repo ==~ /.*rhino.*/) {
			si.exComponent = "JavaScript";
		}else if (ci.repo ==~ /.*ehcache.*/) {
			si.exComponent = "Cache";
		}
		
		si.exStage = getStage(ci.getTime());
		
		si.exTask = resolverTask(ci.comment);
		
		si.exPeriod = getPeriod(ci.time);
		
		if (ci.parents.size()>1) {
			si.exValidation = "N";
		}

		if (ci.comment.startsWith("Merge")) {
			si.exAttribute1 = "Merge";
		}else if (ci.comment.startsWith("Fix")) {
			si.exAttribute1 = "Fix";
		}
		
		if (ci.files!=0 && (ci.linesAdded+ci.linesDeleted)/ci.files > 100) {
			si.exAttribute2 = "大量修改";
		}
		if (ci.files > 100) {
			si.exAttribute2 = "大量文件";
		}

		def realName = RealNameTable.get(ci.author);
		if (null!=realName) {
			si.author = realName;
			si.exAttribute3 = ci.author;	//备份被规范化修改的作者名称
		}
		
	}

	def resolverTask(str){
		def MASK = "#";
		def maxIdx = str.length()-1;
		def start = str.indexOf(MASK);
		if (start >= 0){
			def end = -1;
			for (def i=0; i<10; i++){
				def pointer = start+MASK.length()+i;
				if (pointer > maxIdx){
					break;
				}
				def chr = str[pointer];
				if (chr >= "0" && chr <= "9"){
					end = pointer;
				}else{
					break;
				}
			}
			if (end > start){
				return str[start..end]
			}
		}
		return "";
	}

    def getStage(Date commitTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(commitTime);

        //获得当前日期属于今年的第几周
        calendar.setFirstDayOfWeek(Calendar.MONDAY);//周一作为一周的开始
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);

        //获得指定年度第几周的开始日期/结束日期
        int weekYear = calendar.get(Calendar.YEAR);//获得当前的年
        calendar.setWeekDate(weekYear, weekOfYear, Calendar.MONDAY);//周一作为一周的开始
        def startTime = calendar.getTime();
        calendar.setWeekDate(weekYear, weekOfYear, Calendar.SUNDAY);//周日作为一周的结束
        def endTime = calendar.getTime();

        def _year = DateFormatUtils.format(commitTime, "yyyy");
        def _week = (weekOfYear<10)?"0"+weekOfYear:""+weekOfYear;
        def _firstDate = DateFormatUtils.format(startTime, "MMdd");
        def _lastDate = DateFormatUtils.format(endTime, "MMdd");

        return "${_year}W${_week}(${_firstDate}-${_lastDate})";
    }
	
	def getPeriod(Date commitTime) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(commitTime);
		def dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		if (Calendar.SATURDAY==dayOfWeek || Calendar.SUNDAY==dayOfWeek) {
			return "周末";
		}
		def hour = calendar.get(Calendar.HOUR_OF_DAY);
		if (hour < 8 || hour > 18) {
			return "加班";
		}
		return "正常";
	}
		
	/**
	 * 作者的真名对应关系
	 */
	static def RealNameTable = [
		"勾三股四": "GU Yiling",
		"vue-bot": "-"
	]
}
