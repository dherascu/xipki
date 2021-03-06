/*
 *
 * This file is part of the XiPKI project.
 * Copyright (c) 2014 - 2015 Lijun Liao
 * Author: Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * THE AUTHOR LIJUN LIAO. LIJUN LIAO DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the XiPKI software without
 * disclosing the source code of your own applications.
 *
 * For more information, please contact Lijun Liao at this
 * address: lijun.liao@gmail.com
 */

package org.xipki.security;

import java.nio.charset.StandardCharsets;

import org.xipki.common.util.StringUtil;
import org.xipki.security.api.PasswordResolverException;
import org.xipki.security.api.SinglePasswordResolver;

/**
 * @author Lijun Liao
 */

public class OBFPasswordResolver  implements SinglePasswordResolver
{
    public static final String __OBFUSCATE = "OBF:";

    @Override
    public boolean canResolveProtocol(
            final String protocol)
    {
        return "OBF".equalsIgnoreCase(protocol);
    }

    @Override
    public char[] resolvePassword(
            final String passwordHint)
    throws PasswordResolverException
    {
        return deobfuscate(passwordHint).toCharArray();
    }

    public static String obfuscate(
            final String s)
    {
        StringBuilder buf = new StringBuilder();
        byte[] b = s.getBytes(StandardCharsets.UTF_8);

        buf.append(__OBFUSCATE);
        for (int i = 0; i < b.length; i++)
        {
            byte b1 = b[i];
            byte b2 = b[b.length - (i + 1)];
            if (b1 < 0 || b2 < 0)
            {
                int i0 = (0xff&b1)*256 + (0xff&b2);
                String x = Integer.toString(i0, 36).toLowerCase();
                buf.append("U0000",0,5-x.length());
                buf.append(x);
            }
            else
            {
                int i1 = 127 + b1 + b2;
                int i2 = 127 + b1 - b2;
                int i0 = i1 * 256 + i2;
                String x = Integer.toString(i0, 36).toLowerCase();

                buf.append("000",0,4-x.length());
                buf.append(x);
            }

        }
        return buf.toString();

    }

    /* ------------------------------------------------------------ */
    public static String deobfuscate(
            String s)
    {
        if (StringUtil.startsWithIgnoreCase(s, __OBFUSCATE))
        {
            s = s.substring(4);
        }

        byte[] b = new byte[s.length() / 2];
        int l = 0;
        for (int i = 0; i < s.length(); i += 4)
        {
            if (s.charAt(i)=='U')
            {
                i++;
                String x = s.substring(i, i + 4);
                int i0 = Integer.parseInt(x, 36);
                byte bx = (byte)(i0>>8);
                b[l++] = bx;
            }
            else
            {
                String x = s.substring(i, i + 4);
                int i0 = Integer.parseInt(x, 36);
                int i1 = (i0 / 256);
                int i2 = (i0 % 256);
                byte bx = (byte) ((i1 + i2 - 254) / 2);
                b[l++] = bx;
            }
        }

        return new String(b, 0, l,StandardCharsets.UTF_8);
    }
}
