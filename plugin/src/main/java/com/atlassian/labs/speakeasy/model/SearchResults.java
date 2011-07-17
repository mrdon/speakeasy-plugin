package com.atlassian.labs.speakeasy.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

/**
 *
 */
@XmlRootElement
public class SearchResults
{
    @XmlElement
    private Collection<SearchResult> results;

    public Collection<SearchResult> getResults()
    {
        return results;
    }

    public void setResults(Collection<SearchResult> results)
    {
        this.results = results;
    }
}
