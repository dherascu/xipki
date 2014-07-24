/*
 * Copyright (c) 2014 Lijun Liao
 *
 * TO-BE-DEFINE
 *
 */

package org.xipki.ca.server.mgmt.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.xipki.ca.server.mgmt.PublisherEntry;
import org.xipki.security.common.IoCertUtil;

/**
 * @author Lijun Liao
 */

@Command(scope = "ca", name = "publisher-add", description="Add publisher")
public class PublisherAddCommand extends CaCommand
{

    @Option(name = "-name",
                description = "Required. Publisher Name",
                required = true, multiValued = false)
    protected String name;

    @Option(name = "-type",
            description = "Required. Publisher type",
            required = true)
    protected String type;

    @Option(name = "-conf",
            description = "Publisher configuration")
    protected String conf;

    @Option(name = "-confFile",
            description = "Publisher configuration file")
    protected String confFile;

    @Override
    protected Object doExecute()
    throws Exception
    {
        PublisherEntry entry = new PublisherEntry(name);
        entry.setType(type);

        if(conf == null && confFile != null)
        {
            conf = new String(IoCertUtil.read(confFile));
        }
        if(conf != null)
        {
            entry.setConf(conf);
        }

        caManager.addPublisher(entry);

        return null;
    }
}
