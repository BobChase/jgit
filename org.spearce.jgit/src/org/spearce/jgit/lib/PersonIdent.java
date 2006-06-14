package org.spearce.jgit.lib;

import java.util.Date;
import java.util.TimeZone;

public class PersonIdent
{
    private final String name;

    private final String emailAddress;

    private final long when;

    private final int tzOffset;

    public PersonIdent(final PersonIdent pi)
    {
        this(pi.getName(), pi.getEmailAddress());
    }

    public PersonIdent(final String aName, final String aEmailAddress)
    {
        this(aName, aEmailAddress, new Date(), TimeZone.getDefault());
    }

    public PersonIdent(final PersonIdent pi, final Date when, final TimeZone tz)
    {
        this(pi.getName(), pi.getEmailAddress(), when, tz);
    }

    public PersonIdent(final PersonIdent pi, final Date aWhen)
    {
        name = pi.getName();
        emailAddress = pi.getEmailAddress();
        when = aWhen.getTime();
        tzOffset = pi.tzOffset;
    }

    public PersonIdent(
        final String aName,
        final String aEmailAddress,
        final Date aWhen,
        final TimeZone aTZ)
    {
        name = aName;
        emailAddress = aEmailAddress;
        when = aWhen.getTime();
        tzOffset = aTZ.getOffset(when) / (60 * 1000);
    }

    public PersonIdent(final String in)
    {
        final int lt = in.indexOf('<');
        if (lt == -1)
        {
            throw new IllegalArgumentException("Malformed PersonIdent string"
                + " (no < was found): "
                + in);
        }
        final int gt = in.indexOf('>', lt);
        if (gt == -1)
        {
            throw new IllegalArgumentException("Malformed PersonIdent string"
                + " (no > was found): "
                + in);
        }
        final int sp = in.indexOf(' ', gt + 2);
        if (sp == -1)
        {
            throw new IllegalArgumentException("Malformed PersonIdent string"
                + " (no time zone found): "
                + in);
        }
        final String tzHoursStr = in.substring(sp + 1, sp + 4).trim();
        final int tzHours;
        if (tzHoursStr.charAt(0) == '+')
        {
            tzHours = Integer.parseInt(tzHoursStr.substring(1));
        }
        else
        {
            tzHours = Integer.parseInt(tzHoursStr);
        }
        final int tzMins = Integer.parseInt(in.substring(sp + 4).trim());

        name = in.substring(0, lt).trim();
        emailAddress = in.substring(lt + 1, gt).trim();
        when = Long.parseLong(in.substring(gt + 1, sp).trim()) * 1000;
        tzOffset = tzHours * 60 + tzMins;
    }

    public String getName()
    {
        return name;
    }

    public String getEmailAddress()
    {
        return emailAddress;
    }

    public Date getWhen()
    {
        return new Date(when);
    }

    public String toExternalString()
    {
        final StringBuffer r = new StringBuffer();
        int offset = tzOffset;
        final char sign;
        final int offsetHours;
        final int offsetMins;

        if (offset < 0)
        {
            sign = '-';
            offset = -offset;
        }
        else
        {
            sign = '+';
        }

        offsetHours = offset / 60;
        offsetMins = offset % 60;

        r.append(getName());
        r.append(" <");
        r.append(getEmailAddress());
        r.append("> ");
        r.append(when / 1000);
        r.append(' ');
        r.append(sign);
        if (offsetHours < 10)
        {
            r.append('0');
        }
        r.append(offsetHours);
        if (offsetMins < 10)
        {
            r.append('0');
        }
        r.append(offsetMins);

        return r.toString();
    }

    public String toString()
    {
        final StringBuffer r = new StringBuffer();
        int minutes;

        minutes = tzOffset < 0 ? -tzOffset : tzOffset;
        minutes = (minutes / 100) * 60 + (minutes % 100);
        minutes = tzOffset < 0 ? -minutes : minutes;

        r.append("PersonIdent[");
        r.append(getName());
        r.append(", ");
        r.append(getEmailAddress());
        r.append(", ");
        r.append(new Date(when + minutes * 60));
        r.append("]");

        return r.toString();
    }
}