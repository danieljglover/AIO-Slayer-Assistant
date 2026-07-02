package com.danieljglover.allinslayer.task;

import com.danieljglover.allinslayer.data.SlayerDataService;
import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.Optional;
import net.runelite.api.Client;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskDetectorTest
{
    @Mock
    private Client client;

    private TaskDetector detector;

    @Before
    public void setUp()
    {
        SlayerDataService data = new SlayerDataService(new Gson());
        data.load();
        detector = new TaskDetector(client, data);
    }

    @Test
    public void resolvesCurrentTaskFromTargetVarp()
    {
        when(client.getVarpValue(SlayerVarbits.SLAYER_TARGET)).thenReturn(12);
        when(client.getVarpValue(SlayerVarbits.SLAYER_COUNT)).thenReturn(74);

        Optional<TaskData> task = detector.resolveCurrentTask();

        assertTrue(task.isPresent());
        assertEquals("Abyssal demons", task.get().getTask());
        assertEquals(74, detector.getRemaining());
    }

    @Test
    public void noTaskWhenTargetZero()
    {
        when(client.getVarpValue(SlayerVarbits.SLAYER_TARGET)).thenReturn(0);
        assertFalse(detector.resolveCurrentTask().isPresent());
    }

    @Test
    public void fallsBackToRuneLiteDbTaskNameWhenTargetVarpIsUnmapped()
    {
        when(client.getVarpValue(SlayerVarbits.SLAYER_TARGET)).thenReturn(999);
        when(client.getVarpValue(SlayerVarbits.SLAYER_COUNT)).thenReturn(144);
        when(client.getDBRowsByValue(113, 0, 0, 999)).thenReturn(Collections.singletonList(12345));
        when(client.getDBTableField(12345, 10, 0)).thenReturn(new Object[] {"Greater demons"});

        Optional<TaskData> task = detector.resolveCurrentTask();

        assertTrue(task.isPresent());
        assertEquals("Greater demons", task.get().getTask());
        assertEquals(144, detector.getRemaining());
    }
}
