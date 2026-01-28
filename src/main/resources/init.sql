CREATE SEQUENCE IF NOT EXISTS ScenarioConfig_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS AgentConfig_SEQ START WITH 1 INCREMENT BY 50;


CREATE TABLE IF NOT EXISTS ScenarioConfig (
                                              id bigint NOT NULL,
                                              name varchar(255),
                                              numberOfSlots integer NOT NULL,
                                              maxRounds integer NOT NULL,
                                              PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS AgentConfig (
                                           id bigint NOT NULL,
                                           agentName varchar(255),
                                           strategyType varchar(255),
                                           valuationType varchar(255),
                                           targetSlot integer NOT NULL,
                                           budgetLimit double precision,
                                           scenario_id bigint,
                                           PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS AgentConfig
    ADD CONSTRAINT FK_Agent_Scenario
        FOREIGN KEY (scenario_id)
            REFERENCES ScenarioConfig;

CREATE INDEX IF NOT EXISTS idx_agent_scenario_id ON AgentConfig (scenario_id);

CREATE INDEX IF NOT EXISTS idx_scenario_name ON ScenarioConfig (name);


INSERT INTO ScenarioConfig (id, name, numberOfSlots, maxRounds) VALUES (1, 'General Purpose Cloud', 5, 50);

INSERT INTO AgentConfig (id, agentName, strategyType, valuationType, targetSlot, budgetLimit, scenario_id)
VALUES (10, 'Production_DB_Master', 'MYOPIC', 'RICH', -1, -1, 1);

INSERT INTO AgentConfig (id, agentName, strategyType, valuationType, targetSlot, budgetLimit, scenario_id)
VALUES (11, 'Internal_Wiki_App', 'MYOPIC', 'POOR', -1, -1, 1);

INSERT INTO AgentConfig (id, agentName, strategyType, valuationType, targetSlot, budgetLimit, scenario_id)
VALUES (12, 'Spot_Instance_Bot', 'SNIPER', 'RANDOM', -1, -1, 1);

INSERT INTO AgentConfig (id, agentName, strategyType, valuationType, targetSlot, budgetLimit, scenario_id)
VALUES (13, 'License_Server_Lock', 'MYOPIC', 'FOCUSED', 1, -1, 1);



INSERT INTO ScenarioConfig (id, name, numberOfSlots, maxRounds) VALUES (2, 'HPC Cluster Congestion', 10, 100);
VALUES (20, 'Weather_Simulation_A', 'MYOPIC', 'RICH', -1, -1, 2);

INSERT INTO AgentConfig (id, agentName, strategyType, valuationType, targetSlot, budgetLimit, scenario_id)
VALUES (21, 'Crypto_Miner_Farm', 'MYOPIC', 'RICH', -1, -1, 2);

INSERT INTO AgentConfig (id, agentName, strategyType, valuationType, targetSlot, budgetLimit, scenario_id)
VALUES (22, 'Priority_Scheduler_X', 'SNIPER', 'RICH', -1, -1, 2);

INSERT INTO AgentConfig (id, agentName, strategyType, valuationType, targetSlot, budgetLimit, scenario_id)
VALUES (23, 'Financial_Model_Risk', 'MYOPIC', 'RICH', -1, -1, 2);


INSERT INTO ScenarioConfig (id, name, numberOfSlots, maxRounds) VALUES (3, 'Mixed Workload Optimization', 5, 100);

INSERT INTO AgentConfig (id, agentName, strategyType, valuationType, targetSlot, budgetLimit, scenario_id)
VALUES (30, 'Web_Server_HA', 'FLEXIBLE', 'FLEXIBLE_PAIR', -1, -1, 3);

INSERT INTO AgentConfig (id, agentName, strategyType, valuationType, targetSlot, budgetLimit, scenario_id)
VALUES (31, 'Distributed_ML_Job', 'BUNDLE', 'BUNDLE_PAIR', -1, -1, 3);

INSERT INTO AgentConfig (id, agentName, strategyType, valuationType, targetSlot, budgetLimit, scenario_id)
VALUES (32, 'Dev_Test_Env', 'BUDGET', 'RICH', -1, 15.0, 3);

ALTER SEQUENCE ScenarioConfig_SEQ RESTART WITH 50;
ALTER SEQUENCE AgentConfig_SEQ RESTART WITH 50;