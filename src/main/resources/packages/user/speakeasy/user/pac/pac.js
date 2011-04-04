var $ = require('speakeasy/jquery').jQuery;

exports.init = function() {
    $('#available-extensions-tab').bind('click.loadextensions', function(e) {
        loadAvailableExtensions();
    });
};

function loadAvailableExtensions() {
    $("#loading").show();
    $('#available-extensions-tab').unbind('click.loadextensions');


    var category = 52; // right now this is PAC > JIRA > External Tools, should obviously be switched to "Extensions"
    var numResults = 10;

    // PAC YQL OpenTable stored at https://dl.dropbox.com/u/48692/pac-open-table.xml
    var url = "http://query.yahooapis.com/v1/public/yql?q=use%20'https%3A%2F%2Fdl.dropbox.com%2Fu%2F48692%2Fpac-open-table.xml'%20as%20pac%3B%20SELECT%20item.id%2C%20item.name%2C%20item.pluginKey%2C%20item.icon.location%2C%20item.icon.width%2C%20item.icon.height%2C%20item.latestVersion.version%2C%20item.latestVersion.summary%2C%20item.latestVersion.binaryUrl%2C%20item.vendor.url%2C%20item.vendor.name%20FROM%20pac(0%2C" + numResults + ")%20WHERE%20category%20%3D%20'" + category + "'%3B&format=json&diagnostics=true&callback=?";
    $.getJSON(url,
        function(data) {
            $(data.query.results.json).each(function () { addPACRow(this); });
            $("#loading").hide();
            $("#available-extensions-table").show();
        }
    );
}


function addPACRow(item) {
    var data = {};
    data.id = item.item.id;
    data.key = item.item.pluginKey;
    data.name = item.item.name;
    data.description = item.item.latestVersion.summary;
    data.version = item.item.latestVersion.version;
    data.binaryUrl = item.item.latestVersion.binaryUrl;
    data.author = item.item.vendor.name;
    data.authorUrl = item.item.vendor.url;
//        console.dir(item)

    $('#available-extensions-table').append(require('./row').render(data));
}
