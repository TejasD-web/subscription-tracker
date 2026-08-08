use chrono::NaiveDate;
use colored::Colorize;
use serde::{Deserialize, Serialize};
use std::io::{self, Write};

const BASE_URL: &str = "http://localhost:8080";

// ── Data structures ───────────────────────────────────────────

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
struct Subscription {
    id: u64,
    name: String,
    cost: f64,
    billing_cycle: String,
    category: String,
    subscription_type: String,
    next_renewal_date: NaiveDate,
    trial_end_date: Option<NaiveDate>,
    is_active: bool,
    cancellation_url: Option<String>,
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
struct Analytics {
    total_monthly_spend: f64,
    total_yearly_spend: f64,
    active_subscriptions_count: u32,
}

#[derive(Serialize, Debug)]
#[serde(rename_all = "camelCase")]
struct NewSubscription {
    name: String,
    cost: f64,
    billing_cycle: String,
    category: String,
    subscription_type: String,
    start_date: String,
    next_renewal_date: String,
    reminder_days_before: u32,
    cancellation_url: Option<String>,
    description: Option<String>,
}

// ── Main ─────────────────────────────────────────────────────

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let command = args.get(1).map(|s| s.as_str()).unwrap_or("menu");

    match command {
        "list"      => cmd_list(),
        "analytics" => cmd_analytics(),
        "add"       => cmd_add(),
        "delete"    => cmd_delete(args.get(2)),
        "cancel"    => cmd_cancel(args.get(2)),
        "upcoming"  => cmd_upcoming(),
        "help"      => cmd_help(),
        _           => run_menu(),
    }
}

// ── Interactive menu ─────────────────────────────────────────

fn run_menu() {
    loop {
        print_header();
        println!("{}", "  MAIN MENU".bold().white());
        println!("{}", "  ─────────────────────────────────────────────────────────".white());
        println!("  {}  List all subscriptions", "1.".cyan().bold());
        println!("  {}  Add a subscription", "2.".cyan().bold());
        println!("  {}  View spending analytics", "3.".cyan().bold());
        println!("  {}  Upcoming renewals (next 30 days)", "4.".cyan().bold());
        println!("  {}  Cancel a subscription", "5.".cyan().bold());
        println!("  {}  Delete a subscription", "6.".cyan().bold());
        println!("  {}  Quit", "7.".cyan().bold());
        println!();

        let choice = prompt("  Enter choice (1-7)", None);

        println!();

        match choice.as_str() {
            "1" => cmd_list(),
            "2" => cmd_add(),
            "3" => cmd_analytics(),
            "4" => cmd_upcoming(),
            "5" => cmd_cancel_interactive(),
            "6" => cmd_delete_interactive(),
            "7" | "q" | "quit" => {
                println!("{}", "  Goodbye!".cyan());
                println!();
                break;
            }
            _ => println!("{}", "  Invalid choice — enter a number from 1 to 7.".red()),
        }

        println!();
        println!("{}", "  Press Enter to return to menu...".dimmed());
        let mut buf = String::new();
        io::stdin().read_line(&mut buf).unwrap();
    }
}

// ── Commands ─────────────────────────────────────────────────

fn cmd_list() {
    print_header();
    match fetch_subscriptions() {
        Ok(subs) => {
            if subs.is_empty() {
                println!("{}", "  No subscriptions found. Choose option 2 to add one.".yellow());
            } else {
                print_subscriptions(&subs);
            }
        }
        Err(_) => print_api_error(),
    }
}

fn cmd_analytics() {
    print_header();
    match fetch_analytics() {
        Ok(analytics) => print_analytics(&analytics),
        Err(_) => print_api_error(),
    }
}

fn cmd_upcoming() {
    print_header();
    println!("{}", "  UPCOMING RENEWALS (next 30 days)".bold().white());
    println!("{}", "  ─────────────────────────────────────────────────────────".white());
    match fetch_upcoming() {
        Ok(subs) => {
            if subs.is_empty() {
                println!("{}", "  No renewals in the next 30 days.".yellow());
            } else {
                print_subscriptions(&subs);
            }
        }
        Err(_) => print_api_error(),
    }
}

fn cmd_add() {
    print_header();
    println!("{}", "  ADD SUBSCRIPTION".bold().white());
    println!("{}", "  ─────────────────────────────────────────────────────────".white());
    println!("{}", "  Press Enter to accept defaults shown in [brackets].".dimmed());
    println!();

    let name = prompt("  Name", None);
    if name.is_empty() {
        println!("{}", "  ✗ Name is required.".red());
        return;
    }

    let cost_input = prompt("  Cost (e.g. 9.99)", None);
    let cost: f64 = match cost_input.parse() {
        Ok(v) => v,
        Err(_) => {
            println!("{}", "  ✗ Invalid cost. Enter a number like 9.99.".red());
            return;
        }
    };

    println!("  {}", "MONTHLY, YEARLY, WEEKLY".dimmed());
    let billing_cycle = prompt_with_default("  Billing cycle", "MONTHLY");
    if !["MONTHLY", "YEARLY", "WEEKLY"].contains(&billing_cycle.as_str()) {
        println!("{}", "  ✗ Invalid billing cycle. Use MONTHLY, YEARLY, or WEEKLY.".red());
        return;
    }

    println!("  {}", "STREAMING, MUSIC, FITNESS, SOFTWARE, GAMING, FOOD_DELIVERY, NEWS, CLOUD_STORAGE, OTHER".dimmed());
    let category = prompt_with_default("  Category", "OTHER");

    println!("  {}", "ACTIVE, TRIAL, TRYING_OUT".dimmed());
    let subscription_type = prompt_with_default("  Subscription type", "ACTIVE");

    let today = chrono::Local::now().format("%Y-%m-%d").to_string();
    let start_date = prompt_with_default("  Start date (YYYY-MM-DD)", &today);

    let next_renewal = default_renewal_date(&billing_cycle);
    let next_renewal_date = prompt_with_default("  Next renewal date (YYYY-MM-DD)", &next_renewal);

    let reminder = prompt_with_default("  Reminder days before renewal", "7");
    let reminder_days: u32 = reminder.parse().unwrap_or(7);

    let cancellation_url_input = prompt("  Cancellation URL (optional)", None);
    let cancellation_url = if cancellation_url_input.is_empty() { None } else { Some(cancellation_url_input) };

    let description_input = prompt("  Notes (optional)", None);
    let description = if description_input.is_empty() { None } else { Some(description_input) };

    let new_sub = NewSubscription {
        name,
        cost,
        billing_cycle,
        category,
        subscription_type,
        start_date,
        next_renewal_date,
        reminder_days_before: reminder_days,
        cancellation_url,
        description,
    };

    println!();
    match post_subscription(&new_sub) {
        Ok(sub) => println!("  {} {} added (id: {})", "✓".green().bold(), sub.name.green().bold(), sub.id),
        Err(e) => println!("{}", format!("  ✗ Failed to add: {}", e).red()),
    }
}

fn cmd_cancel_interactive() {
    print_header();
    println!("{}", "  CANCEL SUBSCRIPTION".bold().white());
    println!("{}", "  ─────────────────────────────────────────────────────────".white());
    match fetch_subscriptions() {
        Ok(subs) => {
            if subs.is_empty() {
                println!("{}", "  No subscriptions found.".yellow());
                return;
            }
            print_subscriptions(&subs);
            println!();
            let id = prompt("  Enter subscription ID to cancel", None);
            match cancel_subscription(&id) {
                Ok(_) => println!("  {} Subscription {} marked inactive.", "✓".green().bold(), id),
                Err(e) => println!("{}", format!("  ✗ Failed: {}", e).red()),
            }
        }
        Err(_) => print_api_error(),
    }
}

fn cmd_delete_interactive() {
    print_header();
    println!("{}", "  DELETE SUBSCRIPTION".bold().white());
    println!("{}", "  ─────────────────────────────────────────────────────────".white());
    match fetch_subscriptions() {
        Ok(subs) => {
            if subs.is_empty() {
                println!("{}", "  No subscriptions found.".yellow());
                return;
            }
            print_subscriptions(&subs);
            println!();
            let id = prompt("  Enter subscription ID to permanently delete", None);
            print!("  {} Are you sure? (y/n): ", "⚠".yellow());
            io::stdout().flush().unwrap();
            let mut confirm = String::new();
            io::stdin().read_line(&mut confirm).unwrap();
            if confirm.trim().to_lowercase() == "y" {
                match delete_subscription(&id) {
                    Ok(_) => println!("  {} Subscription {} deleted.", "✓".green().bold(), id),
                    Err(e) => println!("{}", format!("  ✗ Failed: {}", e).red()),
                }
            } else {
                println!("{}", "  Cancelled.".dimmed());
            }
        }
        Err(_) => print_api_error(),
    }
}

fn cmd_delete(id_arg: Option<&String>) {
    match id_arg {
        Some(id) => match delete_subscription(id) {
            Ok(_) => println!("  {} Subscription {} deleted.", "✓".green().bold(), id),
            Err(e) => println!("{}", format!("  ✗ Failed: {}", e).red()),
        },
        None => println!("{}", "  Usage: subcli delete <id>".yellow()),
    }
}

fn cmd_cancel(id_arg: Option<&String>) {
    match id_arg {
        Some(id) => match cancel_subscription(id) {
            Ok(_) => println!("  {} Subscription {} marked inactive.", "✓".green().bold(), id),
            Err(e) => println!("{}", format!("  ✗ Failed: {}", e).red()),
        },
        None => println!("{}", "  Usage: subcli cancel <id>".yellow()),
    }
}

fn cmd_help() {
    print_header();
    println!("{}", "  COMMANDS".bold().white());
    println!("{}", "  ─────────────────────────────────────────────────────────".white());
    println!("  {}         — interactive menu (default)", "subcli".cyan());
    println!("  {}    — list all subscriptions", "subcli list".cyan());
    println!("  {}     — add a new subscription", "subcli add".cyan());
    println!("  {} — show spending analytics", "subcli analytics".cyan());
    println!("  {}  — renewals in the next 30 days", "subcli upcoming".cyan());
    println!("  {} — cancel a subscription", "subcli cancel <id>".cyan());
    println!("  {} — permanently delete", "subcli delete <id>".cyan());
    println!();
    println!("  {}", format!("API: {}", BASE_URL).dimmed());
    println!();
}

// ── API calls ─────────────────────────────────────────────────

fn fetch_subscriptions() -> Result<Vec<Subscription>, reqwest::Error> {
    reqwest::blocking::get(format!("{}/api/subscriptions", BASE_URL))?.json()
}

fn fetch_analytics() -> Result<Analytics, reqwest::Error> {
    reqwest::blocking::get(format!("{}/api/subscriptions/analytics", BASE_URL))?.json()
}

fn fetch_upcoming() -> Result<Vec<Subscription>, reqwest::Error> {
    reqwest::blocking::get(format!("{}/api/subscriptions/upcoming", BASE_URL))?.json()
}

fn post_subscription(sub: &NewSubscription) -> Result<Subscription, reqwest::Error> {
    reqwest::blocking::Client::new()
        .post(format!("{}/api/subscriptions", BASE_URL))
        .json(sub)
        .send()?
        .json()
}

fn delete_subscription(id: &str) -> Result<(), reqwest::Error> {
    reqwest::blocking::Client::new()
        .delete(format!("{}/api/subscriptions/{}", BASE_URL, id))
        .send()?;
    Ok(())
}

fn cancel_subscription(id: &str) -> Result<Subscription, reqwest::Error> {
    reqwest::blocking::Client::new()
        .post(format!("{}/api/subscriptions/{}/cancel", BASE_URL, id))
        .send()?
        .json()
}

// ── Display ───────────────────────────────────────────────────

fn print_header() {
    println!();
    println!("{}", "╔════════════════════════════════════════════╗".cyan());
    println!("{}", "║        SUBSCRIPTION TRACKER CLI            ║".cyan());
    println!("{}", "╚════════════════════════════════════════════╝".cyan());
    println!();
}

fn print_subscriptions(subs: &[Subscription]) {
    println!("{}", "  SUBSCRIPTIONS".bold().white());
    println!("{}", "  ─────────────────────────────────────────────────────────".white());
    for sub in subs {
        let status = match sub.subscription_type.as_str() {
            "TRIAL"      => "TRIAL".yellow(),
            "TRYING_OUT" => "TRYING OUT".magenta(),
            _ => if sub.is_active { "ACTIVE".green() } else { "INACTIVE".red() },
        };
        let cycle = match sub.billing_cycle.as_str() {
            "YEARLY" => "yr",
            "WEEKLY" => "wk",
            _        => "mo",
        };
        println!(
            "  [{}] {:.<32} ${:.2}/{} │ Renews: {} │ [{}] │ {}",
            sub.id.to_string().dimmed(),
            format!("{}  ", sub.name),
            sub.cost,
            cycle,
            sub.next_renewal_date.format("%b %d, %Y"),
            sub.category,
            status
        );
        if let Some(trial_end) = sub.trial_end_date {
            println!("  {}", format!("       ⚠  Trial ends: {}", trial_end.format("%b %d, %Y")).yellow());
        }
        if let Some(ref url) = sub.cancellation_url {
            println!("  {}", format!("       ✗  Cancel at: {}", url).dimmed());
        }
    }
}

fn print_analytics(analytics: &Analytics) {
    println!("{}", "  SPENDING SUMMARY".bold().white());
    println!("{}", "  ─────────────────────────────────────────────────────────".white());
    println!("  Active subscriptions : {}", analytics.active_subscriptions_count.to_string().cyan());
    println!("  Monthly total        : {}", format!("${:.2}", analytics.total_monthly_spend).green().bold());
    println!("  Yearly total         : {}", format!("${:.2}", analytics.total_yearly_spend).green().bold());
}

fn print_api_error() {
    println!("{}", format!("  ✗ Could not reach the API at {}", BASE_URL).red());
    println!("{}", "  Make sure the Spring Boot app is running.".yellow());
}

// ── Prompt helpers ────────────────────────────────────────────

fn prompt(label: &str, _default: Option<&str>) -> String {
    print!("{}: ", label);
    io::stdout().flush().unwrap();
    let mut input = String::new();
    io::stdin().read_line(&mut input).unwrap();
    input.trim().to_string()
}

fn prompt_with_default(label: &str, default: &str) -> String {
    print!("{} [{}]: ", label, default.dimmed());
    io::stdout().flush().unwrap();
    let mut input = String::new();
    io::stdin().read_line(&mut input).unwrap();
    let trimmed = input.trim().to_string();
    if trimmed.is_empty() { default.to_string() } else { trimmed }
}

fn default_renewal_date(billing_cycle: &str) -> String {
    let today = chrono::Local::now();
    let next = match billing_cycle {
        "YEARLY" => today + chrono::Duration::days(365),
        "WEEKLY" => today + chrono::Duration::days(7),
        _        => today + chrono::Duration::days(30),
    };
    next.format("%Y-%m-%d").to_string()
}
