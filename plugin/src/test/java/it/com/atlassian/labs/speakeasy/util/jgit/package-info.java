/**
 * All this crap is necessary as JIRA insists on sticking the character encoding on every content type returned,
 * screwing up jgit, which is doing an exact match on the value and failing.  I switched the two spots to use contains()
 * instead of equals().  All other changes were necessary to get it to compile due to protected and package-scoped stuff.
 */
package it.com.atlassian.labs.speakeasy.util.jgit;
