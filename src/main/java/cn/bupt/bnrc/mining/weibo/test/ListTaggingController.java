package cn.bupt.bnrc.mining.weibo.test;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cn.bupt.bnrc.mining.weibo.tagging.TaggingStatusesService;

@Controller
@RequestMapping(value="/tagging")
public class ListTaggingController {

	@RequestMapping(value="statuses-list")
	public String taggingStatuses(HttpServletRequest request, HttpServletResponse response,
			Model model) throws Exception{
		
		int pageNum = 0;
		if (request.getParameter("page") != null){
			pageNum = new Integer(request.getParameter("page"));
		}
		
		logger.info(String.format("statuses list -- page: %d", pageNum));
		
		List<Map<String, Object>> statuses = taggingStatusesService.getPagedStatuses(pageNum);
		model.addAttribute("statuses", statuses);
		model.addAttribute("totalPage", taggingStatusesService.getUntaggedStatusesPageCount());
		model.addAttribute("current", pageNum);
		return "tagging/status-tagging";
	}
	
	@RequestMapping(value="update", params={"id", "flag"})
	public void updateStatusTagFlag(@RequestParam("id") int id, @RequestParam("flag") int taggingFlag,
			HttpServletRequest request, HttpServletResponse response){
		logger.info(String.format("update status -- id: %d, flag: %d", id, taggingFlag));
		taggingStatusesService.updateStatusTagFlag(id, taggingFlag);
	}
	
	@Autowired private TaggingStatusesService taggingStatusesService;
	private Logger logger = LoggerFactory.getLogger(getClass());
}
