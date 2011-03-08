/**
 * Executes Confluence remote API methods
 *
 * @public
 */
var $ = require('./jquery').jQuery;
var ServiceProxy = require('./xmlrpc').ServiceProxy;
var service = new ServiceProxy(contextPath + "/rpc/xmlrpc", {
                        protocol: "XML-RPC",
                        methods: ['confluence1.getBlogEntries',
                                  'confluence1.getBlogEntry',
                                  'confluence1.getLabelsById']});


function getBlogSummaries(spaceKey, callback) {
    service.confluence1.getBlogEntries({
        params : [null, spaceKey],
        onSuccess : function(result) { callback(result); }
    });
}

function getBlogEntry(id, callback) {
    service.confluence1.getBlogEntry({
        params : [null, id],
        onSuccess : function(result) { callback(result); }
    });
}

function getLabelsById(contentId, callback) {
    service.confluence1.getLabelsById({
        params : [null, contentId],
        onSuccess : function(result) { callback(result); }
    });
}

function getBlogsInPeriod(spaceKey, start, end, callback) {
    getBlogSummaries(spaceKey, function(summaries) {
        fetchForList(summaries, {
            shouldRetrieve : function(summary) { return summary.publishDate > start && summary.publishDate < end; },
            retrieve : function(summary, callback) {
                getBlogEntry(summary.id, function(blog) {
                    blog.publishDate = summary.publishDate;
                    callback(blog);
                });
            },
            success : function(blogs) {
                blogs.sort(function (a, b) {
                    return (a.publishDate < b.publishDate) ?  -1 : (a.publishDate > b.publishDate) ? 1 : 0
                });
                fetchForList(blogs, {
                    shouldRetrieve : function(blog) { return true; },
                    retrieve : function(blog, callback) {
                        getLabelsById(blog.id, function(labels) {
                            blog.labels = labels ? labels : [];
                            callback(blog);
                        });
                    },
                    success : function(blogs) {
                        callback(blogs);
                    }
                });
            }
        });
    });
}

function fetchForList(list, callbacks) {
    var retrieved = [];
    var numToRetrieve = 0;
    var doneQueuing = false;

    function checkFinished() {
        if (doneQueuing && retrieved.length == numToRetrieve) {
            callbacks.success(retrieved);
        }
        else {
            //console.log("Waiting for " + (numToRetrieve - retrieved.length) + " more. " + doneQueuing + " " + numToRetrieve + " " + retrieved.length);
        }
    }

    $.each(list, function(id, entry) {

        if (callbacks.shouldRetrieve(entry)) {
            numToRetrieve++;
            callbacks.retrieve(entry, function(object) {
                retrieved.push(object);
                checkFinished();
            });
        }
    });
    doneQueuing = true;
    checkFinished();
}

/**
 * Gets a list of <a href="http://confluence.atlassian.com/display/CONFDEV/Remote+API+Specification#RemoteAPISpecification-BlogEntrySummary">BlogEntrySummaries</a>
 * entries in the space. Parameters:
 * <ul>
 *     <li><code>spaceKey</code> - The space key</li>
 *     <li><code>callback</code> - The function to call when the data is read</li>
 * </ul>
 */
exports.getBlogSummaries = getBlogSummaries;
/**
 * Gets a <a href="http://confluence.atlassian.com/display/CONFDEV/Remote+API+Specification#RemoteAPISpecification-BlogEntry">BlogEntry</a>.
 * Parameters:
 * <ul>
 *     <li><code>pageId</code> - The blog (page) id</li>
 *     <li><code>callback</code> - The function to call when the data is read</li>
 * </ul>
 */
exports.getBlogEntry = getBlogEntry;
/**
 * Gets a list of <a href="http://confluence.atlassian.com/display/CONFDEV/Remote+API+Specification#RemoteAPISpecification-BlogEntry">BlogEntry</a>
 * entries in the space with labels. Parameters:
 * <ul>
 *     <li><code>spaceKey</code> - The space key</li>
 *     <li><code>start</code> - The start date</li>
 *     <li><code>end</code> - The end date</li>
 *     <li><code>callback</code> - The function to call when the data is read</li>
 * </ul>
 */
exports.getBlogsInPeriod = getBlogsInPeriod;
/**
 * Gets a list of <a href="http://confluence.atlassian.com/display/CONFDEV/Remote+API+Specification#RemoteAPISpecification-Label">Label</a>
 * entries for the content id. Parameters:
 * <ul>
 *     <li><code>contentId</code> - The content id</li>
 *     <li><code>callback</code> - The function to call when the data is read</li>
 * </ul>
 */
exports.getLabelsById = getLabelsById;