<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="cn.bupt.bnrc.mining.weibo.tagging.TaggingStatusesService" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<html>
<head>
	<title>话题情感倾向统计</title>
	<script src="${ctx}/static/highcharts/highcharts.js" type="text/javascript"></script>
	<script src="${ctx}/static/highcharts/modules/exporting.js" type="text/javascript"></script>
</head>

<body>
	<c:if test="${not empty message}">
		<div id="message" class="alert alert-success"><button data-dismiss="alert" class="close">×</button>${message}</div>
	</c:if>
	
	<div id="percentContainer" style="min-width: 400px; height: 400px; margin: 0 auto"></div>
	<div id="numTrendContainer" style="min-width: 400px; height: 400px; margin: 0 auto"></div>
	
	<script>
	$(function () {
	    var numTrendChart;
	    var percentChart;
	    $(document).ready(function() {
	    	percentChart = new Highcharts.Chart({
	            chart: {
	                renderTo: 'percentContainer',
	                plotBackgroundColor: null,
	                plotBorderWidth: null,
	                plotShadow: false
	            },
	            title: {
	                text: '话题情感倾向统计'
	            },
	            tooltip: {
	        	    pointFormat: '{series.name}: <b>{point.percentage}%</b>',
	            	percentageDecimals: 1
	            },
	            plotOptions: {
	                pie: {
	                    allowPointSelect: true,
	                    cursor: 'pointer',
	                    dataLabels: {
	                        enabled: true,
	                        color: '#000000',
	                        connectorColor: '#000000',
	                        formatter: function() {
	                            return '<b>'+ this.point.name +'</b>: '+ this.percentage +' %';
	                        }
	                    }
	                }
	            },
	            series: [{
	                type: 'pie',
	                name: '数量百分比',
	                data: [
	                    ['Firefox',   45.0],
	                    ['IE',       26.8],
	                    {
	                        name: 'Chrome',
	                        y: 12.8,
	                        sliced: true,
	                        selected: true
	                    },
	                    ['Safari',    8.5],
	                    ['Opera',     6.2],
	                    ['Others',   0.7]
	                ]
	            }]
	        });
	        numTrendChart = new Highcharts.Chart({
	            chart: {
	                renderTo: 'numTrendContainer',
	                type: 'line',
	                marginRight: 130,
	                marginBottom: 25
	            },
	            title: {text: '主观微博的数量变化图', x: -20},
	            xAxis: {categories: 
	            	['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']},
	            yAxis: {
	                title: {text: '微博数量'},
	                plotLines: [{value: 0, width: 1, color: '#808080'}]
	            },
	            tooltip: {
	                formatter: function() {
	                        return '<b>'+ this.series.name +'</b><br/>'+
	                        this.x +': '+ this.y +'°C';
	                }},
	            legend: {
	                layout: 'vertical',
	                align: 'right',
	                verticalAlign: 'top',
	                x: -10,
	                y: 100,
	                borderWidth: 0
	            },
	            series: [{
	                name: '积极情感的微博数量增量',
	                data: [-0.9, 0.6, 3.5, 8.4, 13.5, 17.0, 18.6, 17.9, 14.3, 9.0, 3.9, 1.0]
	            }, {
	                name: '消极情感的微博数量增量',
	                data: [3.9, 4.2, 5.7, 8.5, 11.9, 15.2, 17.0, 16.6, 14.2, 10.3, 6.6, 4.8]
	            }]
	        });
	    });
	});
    </script>
</body>