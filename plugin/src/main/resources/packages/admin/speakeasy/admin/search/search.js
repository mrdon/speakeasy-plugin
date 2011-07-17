var $ = require('speakeasy/jquery').jQuery;
var messages = require('speakeasy/messages');
var _ = require('underscore');
var tmpl = require('./result');
var host = require('speakeasy/host');
var spinner = require('speakeasy/spinner');

function getAbsoluteHref($link) {
    var href = $link.attr('href') || $link.attr('data-href');
    return contextPath + href;
}

function updateSearchResults(data) {
    var $results = $('#sp-search-results');
    $results.empty();
    _.each(data.results, function(result) {
        $results.append(tmpl.render(result));
    });
    if (data.results.length == 0) {
        $results.append('<div>No search results found</div>');
    }
}
function getErrorMessage(xhr) {
    return xhr.responseText.indexOf("{") == 0 ? $.parseJSON(xhr.responseText).message : xhr.responseText;
}

function executeSearch(button, text) {
    var oldButtonHtml = spinner.replace(button);
    $.ajax({
              url: getAbsoluteHref(button),
              data: {'q' : text },
              type: 'POST',
              beforeSend: function(jqXHR, settings) {
                jqXHR.setRequestHeader("X-Atlassian-Token", "nocheck");
              },
              success: function(data) {
                  updateSearchResults(data);
                  button.html(oldButtonHtml);
              },
              error: function(xhr) {
                  messages.add('error', {title: "Error searching", body: getErrorMessage(xhr), shadowed: false});
                  button.html(oldButtonHtml);
              }
            });
}


exports.init = function() {
    $('#sp-search-submit').click(function(e) {
        e.preventDefault();
        $.data($('#sp-search-form')[0], AJS.DIRTY_FORM_VALUE, null);
        executeSearch($(e.target), $('#sp-search-field').val());
    });
};