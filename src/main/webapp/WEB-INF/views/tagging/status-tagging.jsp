<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="cn.bupt.bnrc.mining.weibo.tagging.TaggingStatusesService" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<html>
<head>
	<title>微博数据极性标记</title>
</head>

<body>
	<c:if test="${not empty message}">
		<div id="message" class="alert alert-success"><button data-dismiss="alert" class="close">×</button>${message}</div>
	</c:if>

	<table id="contentTable" class="table table-striped table-bordered table-condensed">
		<thead>
			<tr>
				<th>id</th>
				<th>微博内容</th>
				<th>标记</th>
			</tr>
		</thead>
		<tbody>
		<c:forEach items="${statuses}" var="status">
			<tr>
				<td>
					${status['id']}
				</td>
				<td>
					${status['content']}
				</td>
				<td style="width: 100px;" >
					<div><a class="update-flag" href="${ctx}/tagging/update?id=${status['id']}&flag=<%=TaggingStatusesService.TAG_NETURAL_FLAG%>">客观类</a></div>
					<div><a class="update-flag" href="${ctx}/tagging/update?id=${status['id']}&flag=<%=TaggingStatusesService.TAG_POSITIVE_FLAG%>">积极情感类</a></div>
					<div><a class="update-flag" href="${ctx}/tagging/update?id=${status['id']}&flag=<%=TaggingStatusesService.TAG_NEGATIVE_FLAG%>">消极情感类</a></div>
				</td>
			</tr>
		</c:forEach>
		</tbody>
	</table>

	<script>
		$(document).ready(function(){
			$(".update-flag").click(function(event){
				var current = $(this);
				$.ajax({
					type: "POST",
					url : current.attr("href"),
					success:function(result){current.css("color","black");},
					error:function(result){}
				});
				event.preventDefault();
			});
		});
	</script>

	<tags:pagination totalPage="${totalPage}" paginationSize="5" current="${current}" />

</body>
</html>