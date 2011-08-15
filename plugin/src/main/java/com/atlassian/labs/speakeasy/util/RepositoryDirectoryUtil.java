package com.atlassian.labs.speakeasy.util;

import bsh.commands.dir;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

/**
 *
 */
public class RepositoryDirectoryUtil
{
    private static final FilenameFilter NO_GIT = new FilenameFilter()
    {
        public boolean accept(File dir, String name)
        {
            return !name.equals(".git");
        }
    };

    public static List<String> getEntries(File root)
    {
        return walk(root.toURI(), root);
    }
    public static List<String> walk(URI root, File dir)
    {
        TreeMap<String,List<String>> dirs = new TreeMap<String, List<String>>();
        TreeMap<String, File> files = new TreeMap<String, File>();
        for (File entry : dir.listFiles(NO_GIT))
        {
            String path = entry.getName();
            if (entry.isDirectory())
            {
                dirs.put(path + "/", walk(root, entry));
            }
            else
            {
                files.put(path, entry);
            }
        }
        List<String> contents = newArrayList();
        for (String dirName : dirs.keySet())
        {
            contents.add(root.relativize(new File(dir, dirName).toURI()).getPath());
            contents.addAll(dirs.get(dirName));
        }
        for (File file : files.values())
        {
            contents.add(root.relativize(file.toURI()).getPath());
        }
        return contents;
    }

    private static String stackToPath(List<File> stack, File leaf)
    {
        StringBuilder sb = new StringBuilder();
        for (int x=1; x < stack.size() && stack.size() > 1; x++)
        {
            File cur = stack.get(x);
            addEntry(sb, cur);
        }
        addEntry(sb, leaf);
        return sb.toString();
    }

    private static void addEntry(StringBuilder sb, File cur)
    {
        sb.append(cur.getName());
        if (cur.isDirectory())
        {
            sb.append("/");
        }
    }
}
