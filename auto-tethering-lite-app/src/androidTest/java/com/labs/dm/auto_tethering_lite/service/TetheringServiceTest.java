package com.labs.dm.auto_tethering_lite.service;

import android.content.Intent;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Created by Daniel Mroczka on 15-Dec-16.
 */
public class TetheringServiceTest extends ServiceTestCase<TetheringService> {

    public TetheringServiceTest() {
        super(TetheringService.class);
    }

    @SmallTest
    public void testOnCreate() throws Exception {
        Intent intent = new Intent(getContext(), TetheringService.class);
        startService(intent);
        assertNotNull(getService());
    }


}