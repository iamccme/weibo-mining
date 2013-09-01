package cn.bupt.bnrc.mining.weibo.test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value="index")
public class IndexController {

	@RequestMapping(method=RequestMethod.GET)
	public String index(){
		return "index";
	}
}
