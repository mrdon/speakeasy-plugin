package it.com.atlassian.labs.speakeasy;

import com.atlassian.mail.server.MailServer;
import com.atlassian.plugin.util.WaitUntil;
import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MailUtils
{
    private static final Logger log = LoggerFactory.getLogger(MailUtils.class);

    public static String assertEmailExists(final SimpleSmtpServer mailServer, String to, String title, List<String> bodyStrings) throws MessagingException, IOException
    {

        final AtomicReference<SmtpMessage> ref = new AtomicReference<SmtpMessage>();
        WaitUntil.invoke(new WaitUntil.WaitCondition()
        {
            public boolean isFinished()
            {
                Iterator itr = mailServer.getReceivedEmail();
                while (itr.hasNext())
                {
                    ref.set((SmtpMessage) itr.next());
                }
                return ref.get() != null;
            }

            public String getWaitMessage()
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        Assert.assertTrue(ref.get() != null);

        SmtpMessage lastMessage = ref.get();
        log.error("msg: " + lastMessage.toString());
        String subject = lastMessage.getHeaderValue("Subject");
        Assert.assertTrue(subject.startsWith("[test] " + title));
        Assert.assertFalse(subject.contains("$"));
        String body = lastMessage.getBody();
        Assert.assertFalse(body.contains("$"));
        for (String toMatch : bodyStrings)
        {
            if (!body.contains(toMatch))
            {
                Assert.fail("Couldn't match '" + toMatch + "' in:\n" + body);
            }
        }

        Assert.assertTrue(lastMessage.getHeaderValue("To").contains(to));
        return body;
    }
}