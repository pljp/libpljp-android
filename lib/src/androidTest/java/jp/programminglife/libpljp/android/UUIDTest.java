package jp.programminglife.libpljp.android;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class UUIDTest {

    @Test
    public void generateTest() {

        final Context context = InstrumentationRegistry.getContext();
        final UUIDGenerator uuidGenerator = new UUIDGenerator(context);
        long time = System.currentTimeMillis();
        for (int i=0; i<=0x4000; i++) {
            String uuid;
            try {
                uuid = uuidGenerator.generateString(time);
                if ( (i & 0xfff) == 0 || (i & 0xfff) == 0xfff )
                    System.out.println(uuid);
            }
            catch(IllegalStateException e) {
                System.out.println(Integer.toString(i));
                Assert.assertEquals(i, 0x4000);
            }
        }

    }

}
