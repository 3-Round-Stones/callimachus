// chart.js

(function($) {

$(document).ready(function () {
	$(".chart").each(loadChart);
});

$(document).bind('DOMNodeInserted', function (event) {
	$(event.target).find(".chart").andSelf().filter(".chart").each(loadChart);
});

var counter = 0;

function loadChart() {
	var div = $(this);
	var id = div.attr('id');
	var chartType = div.attr('data-chart-type');
	var chartSource = div.attr('data-chart-source');
	var chartOptions = div.attr('data-chart-options');

	if (chartType && chartSource) {
		var options = chartOptions ? eval('({' + chartOptions + '})') : {};
		if (!options.width) {
			options.width = div.width();
		}
		if (!options.height) {
			options.height = div.height();
		}
		if (!id) {
			id = 'chart' + (++counter);
			div.attr('id', id);
		}

		google.load('visualization', '1.0', {packages: ['charteditor'], callback: function(){
			var chart = new google.visualization.ChartWrapper();
			chart.setChartType(chartType);
			chart.setDataSourceUrl(chartSource);
			chart.setContainerId(id);
			chart.setOptions(options);
			chart.draw();
		}});
	}
}

})(jQuery);
