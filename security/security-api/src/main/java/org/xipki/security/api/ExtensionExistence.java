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

package org.xipki.security.api;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.xipki.common.util.CollectionUtil;

/**
 *
 * <pre>
 * ExtensionExistence ::= SEQUENCE
 * {
 *     needExtensions [0] ExtensionTypes EXPLICIT OPTIONAL,
 *     wantExtensions [1] ExtensionTypes EXPLICIT OPTIONAL,
 * }
 *
 * ExtensionTypes ::= SEQUENCE OF OBJECT IDENTIFIER
 * </pre>
 *
 * @author Lijun Liao
 */

public class ExtensionExistence extends ASN1Object
{
    private List<ASN1ObjectIdentifier> needExtensions;
    private List<ASN1ObjectIdentifier> wantExtensions;

    public ExtensionExistence(
            final List<ASN1ObjectIdentifier> needExtensions,
            List<ASN1ObjectIdentifier> wantExtensions)
    {
        this.needExtensions = needExtensions;
        this.wantExtensions = wantExtensions;

        if(this.needExtensions == null)
        {
            List<ASN1ObjectIdentifier> list = Collections.emptyList();
            this.needExtensions = Collections.unmodifiableList(list);
        }

        if(this.wantExtensions == null)
        {
            List<ASN1ObjectIdentifier> list = Collections.emptyList();
            this.wantExtensions = Collections.unmodifiableList(list);
        }

    }

    private ExtensionExistence(
            final ASN1Sequence seq)
    {
        int size = seq.size();
        if (size > 2)
        {
            throw new IllegalArgumentException("wrong number of elements in sequence");
        }

        for(int i = 0; i < size; i++)
        {
            ASN1TaggedObject tagObject = ASN1TaggedObject.getInstance(seq.getObjectAt(i));
            int tag = tagObject.getTagNo();
            if(tag == 0 || tag == 1)
            {
                ASN1Sequence subSeq = ASN1Sequence.getInstance(tagObject.getObject());
                List<ASN1ObjectIdentifier> oids = new LinkedList<>();
                int subSize = subSeq.size();
                for(int j = 0; j < subSize; j++)
                {
                    oids.add(ASN1ObjectIdentifier.getInstance(subSeq.getObjectAt(j)));
                }

                if(tag == 0)
                {
                    needExtensions = Collections.unmodifiableList(oids);
                } else
                {
                    wantExtensions = Collections.unmodifiableList(oids);
                }
            } else
            {
                throw new IllegalArgumentException("tag " + tag + " is not permitted");
            }
        }

        if(needExtensions == null)
        {
            List<ASN1ObjectIdentifier> list = Collections.emptyList();
            needExtensions = Collections.unmodifiableList(list);
        }

        if(wantExtensions == null)
        {
            List<ASN1ObjectIdentifier> list = Collections.emptyList();
            wantExtensions = Collections.unmodifiableList(list);
        }

    }

    public static ExtensionExistence getInstance(
            final Object obj)
    {
        if (obj == null || obj instanceof ExtensionExistence)
        {
            return (ExtensionExistence)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new ExtensionExistence((ASN1Sequence) obj);
        }

        if (obj instanceof byte[])
        {
            try
            {
                return getInstance(ASN1Primitive.fromByteArray((byte[])obj));
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("unable to parse encoded general name");
            }
        }

        throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
    }

    @Override
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        if(CollectionUtil.isNotEmpty(needExtensions))
        {
            ASN1EncodableVector v = new ASN1EncodableVector();
            for(ASN1ObjectIdentifier m : needExtensions)
            {
                v.add(m);
            }
            vector.add(new DERTaggedObject(true, 0, new DERSequence(v)));
        }

        if(CollectionUtil.isNotEmpty(wantExtensions))
        {
            ASN1EncodableVector v = new ASN1EncodableVector();
            for(ASN1ObjectIdentifier m : wantExtensions)
            {
                v.add(m);
            }
            vector.add(new DERTaggedObject(true, 1, new DERSequence(v)));
        }

        return new DERSequence(vector);
    }

    public List<ASN1ObjectIdentifier> getNeedExtensions()
    {
        return needExtensions;
    }

    public List<ASN1ObjectIdentifier> getWantExtensions()
    {
        return wantExtensions;
    }

}
