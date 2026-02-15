-- Create SAGA instances table
CREATE TABLE saga_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    current_step_index INTEGER DEFAULT 0,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    version BIGINT DEFAULT 0
);

-- Create SAGA steps table
CREATE TABLE saga_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    step_name VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    compensation_action VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    step_data TEXT,
    saga_instance_id UUID NOT NULL,
    
    CONSTRAINT fk_saga_steps_instance_id 
        FOREIGN KEY (saga_instance_id) REFERENCES saga_instances(id)
        ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_saga_instances_aggregate_id ON saga_instances(aggregate_id);
CREATE INDEX idx_saga_instances_status ON saga_instances(status);
CREATE INDEX idx_saga_instances_saga_type ON saga_instances(saga_type);
CREATE INDEX idx_saga_instances_started_at ON saga_instances(started_at);

CREATE INDEX idx_saga_steps_instance_id ON saga_steps(saga_instance_id);
CREATE INDEX idx_saga_steps_status ON saga_steps(status);
CREATE INDEX idx_saga_steps_step_name ON saga_steps(step_name);

-- Create composite indexes for common queries
CREATE INDEX idx_saga_instances_status_type ON saga_instances(status, saga_type);
CREATE INDEX idx_saga_steps_instance_status ON saga_steps(saga_instance_id, status);
